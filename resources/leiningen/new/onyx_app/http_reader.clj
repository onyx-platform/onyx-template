(ns {{app-name}}.plugins.http-reader
  (:require [clojure.core.async :refer [chan >!! <!! alts!! timeout go <!]]
            [onyx.peer.pipeline-extensions :as p-ext]
            [onyx.static.default-vals :refer [defaults]]))

(defn inject-reader
  [{:keys [onyx.core/task-map]} lifecycle]
  (let [ch (chan 10000)
        fut
        (future
          (try
            (with-open [rdr (clojure.java.io/reader (:http/uri task-map))]
              (doseq [x (line-seq rdr)]
                (>!! ch {:line x}))
              (>!! ch :done))
            (catch Exception e
              (.printStackTrace e))))]
    {:http/fut fut
     :http/chan ch
     :http/retry-ch (chan 1000)
     :http/drained? (atom false)
     :http/pending-messages (atom {})}))

(def reader-calls
  {:lifecycle/before-task-start inject-reader})

(defmethod p-ext/read-batch :http/read-lines
  [{:keys [onyx.core/task-map http/chan http/retry-ch 
           http/pending-messages http/drained?] :as event}]
  (let [pending (count @pending-messages)
        max-pending (or (:onyx/max-pending task-map) (:onyx/max-pending defaults))
        batch-size (:onyx/batch-size task-map)
        max-segments (min (- max-pending pending) batch-size)
        ms (or (:onyx/batch-timeout task-map) (:onyx/batch-timeout defaults))
        step-ms (/ ms (:onyx/batch-size task-map))
        timeout-ch (timeout ms)
        batch (if (zero? max-segments)
                (<!! timeout-ch)
                (loop [segments [] cnt 0]
                  (if (= cnt batch-size)
                    segments
                    (if-let [message (first (alts!! [retry-ch chan timeout-ch] :priority true))] 
                      (recur (conj segments 
                                   {:id (java.util.UUID/randomUUID)
                                    :input :http
                                    :message message})
                             (inc cnt))
                      segments))))]
    (doseq [m batch]
      (swap! pending-messages assoc (:id m) (:message m)))
    (when (and (= 1 (count @pending-messages))
               (= (count batch) 1)
               (= (:message (first batch)) :done))
      (reset! drained? true))
    {:onyx.core/batch batch}))

(defmethod p-ext/ack-message :http/read-lines
  [{:keys [http/pending-messages]} message-id]
  (swap! pending-messages dissoc message-id))

(defmethod p-ext/retry-message :http/read-lines
  [{:keys [http/pending-messages http/retry-ch]} message-id]
  (when-let [msg (get @pending-messages message-id)]
    (>!! retry-ch msg)
    (swap! pending-messages dissoc message-id)))

(defmethod p-ext/pending? :http/read-lines
  [{:keys [http/pending-messages]} message-id]
  (get @pending-messages message-id))

(defmethod p-ext/drained? :http/read-lines
  [{:keys [http/drained? http/pending-messages] :as event}]
  @drained?)
