(ns {{app-name}}.lifecycles.sample-lifecycle
    (:require [clojure.core.async :refer [chan sliding-buffer >!!]]
              [onyx.plugin.core-async :refer [take-segments!]]
              [taoensso.timbre :refer [info]]
              [clojure.set :refer [join]]
              [cheshire.core :as json]))

;;;;======================================================
;;;;                 Logging

(defn log-batch [event lifecycle]
  (let [task-name (:onyx/name (:onyx.core/task-map event))]
    (doseq [m (map :message (mapcat :leaves (:tree (:onyx.core/results event))))]
      (info task-name " logging segment: " m)))
  {})

(def log-calls
  {:lifecycle/after-batch log-batch})

(defn add-logging
  "Add's logging output to a tasks output-batch. "
  [job task]
  (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
    (-> job
        (update-in [:lifecycles] conj {:lifecycle/task task
                                       :lifecycle/calls ::log-calls}))))

;;;;======================================================
;;;;                 Core Async
(def channels (atom {}))

(defn get-channel
  ([id] (get-channel id nil))
  ([id size]
   (if-let [id (get @channels id)]
     id
     (do (swap! channels assoc id (chan (or size 1000)))
         (get-channel id)))))

(defn inject-in-ch
  [_ lifecycle]
  {:core.async/chan (get-channel (:core.async/id lifecycle)
                                 (or (:core.async/size lifecycle) 1000))})
(defn inject-out-ch
  [_ lifecycle]
  {:core.async/chan (get-channel (:core.async/id lifecycle)
                                 (or (:core.async/size lifecycle) 1001))})

(def in-calls
  {:lifecycle/before-task-start inject-in-ch})

(def out-calls
  {:lifecycle/before-task-start inject-out-ch})

(defn get-core-async-channels
  [{:keys [catalog lifecycles]}]
  (let [lifecycle-catalog-join (join catalog lifecycles {:onyx/name :lifecycle/task})]
    (reduce (fn [acc item]
              (assoc acc
                     (:onyx/name item)
                     (get-channel (:core.async/id item)))) {} (filter :core.async/id lifecycle-catalog-join))))


(defn add-core-async-input
  ([job task] (add-core-async-input job task 1000))
  ([job task chan-size]
   (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
     (-> job
         (update-in [:lifecycles] into [{:lifecycle/task task
                                         :lifecycle/calls ::in-calls
                                         :core.async/id   (java.util.UUID/randomUUID)
                                         :core.async/size chan-size}
                                        {:lifecycle/task task
                                         :lifecycle/calls :onyx.plugin.core-async/reader-calls}]))
     (throw (java.lang.IllegalArgumentException)))))

(defn add-core-async-output
  ([job task] (add-core-async-output job task 1000))
  ([job task chan-size]
   (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
     (update-in job [:lifecycles] into [{:lifecycle/task task
                                         :core.async/id   (java.util.UUID/randomUUID)
                                         :core.async/size (inc chan-size)
                                         :lifecycle/calls ::out-calls}
                                        {:lifecycle/task task
                                         :lifecycle/calls :onyx.plugin.core-async/writer-calls}]))))

;;;;======================================================
;;;;                 Kafka
(defn deserialize-message-json [bytes]
  (try
    (json/parse-string (String. bytes "UTF-8"))
    (catch Exception e
      {:error e})))

(defn deserialize-message-edn [bytes]
  (try
    (read-string (String. bytes "UTF-8"))
    (catch Exception e
      {:error e})))

(defn serialize-message-json [segment]
  (.getBytes (json/generate-string segment)))

(defn serialize-message-edn [segment]
  (.getBytes segment))

(defn expand-serializer-fn [task]
  (update-in task [:kafka/serializer-fn]
             (fn [v]
               (condp = v
                 :json    ::serialize-message-json
                 :edn     ::serialize-message-edn
                 v))))

(defn expand-deserializer-fn [task]
  (update-in task [:kafka/deserializer-fn]
             (fn [v]
               (condp = v
                 :json    ::deserialize-message-json
                 :edn     ::deserialize-message-edn
                 v))))

(defn add-kafka-input
  "Instrument a job with Kafka lifecycles and catalog entries.
  opts are of the following form for Kafka consumers AND producers

  opts {
  :kafka/topic               - Name of a topic
  :kafka/partition           - Optional: partition to read from if
                                 auto-assignment is not used
  :kafka/group-id            - The consumer identity to store in ZooKeeper
  :kafka/zookeeper           - The ZooKeeper connection string
  :kafka/offset-reset        - Offset bound to seek to when not found
                                 - :smallest or :largest
  :kafka/force-reset?        - Force to read from the beginning or end of the
                                 log, as specified by :kafka/offset-reset.
                                 If false, reads from the last acknowledged
                                 messsage if it exists
  :kafka/deserializer-fn     - :json or :edn for default deserializers, a
                                custom fn can also be supplied. Only for
                                :input tasks}

  ========================== Optional Settings =================================
  :kafka/chan-capacity
  :kafka/fetch-size
  :kafka/empty-read-back-off
  :kafka/commit-interval
  :kafka/request-size"
  ([job task] (add-kafka-input job task nil))
  ([job task opts]
   (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
     (-> job
         (update-in [:lifecycles] conj {:lifecycle/task task
                                        :lifecycle/calls :onyx.plugin.kafka/read-messages-calls})
         (update-in [:catalog] (fn [catalog]
                                 (replace {entry ((comp expand-deserializer-fn
                                                        (partial merge opts)) entry)} catalog))))
     (throw (java.lang.IllegalArgumentException)))))

(defn add-kafka-output
  "Instrument a job with Kafka lifecycles and catalog entries.
  opts are of the following form for Kafka consumers AND producers

  :kafka/topic               - Name of a topic
  :kafka/zookeeper           - The ZooKeeper connection string
  :kafka/serializer-fn       - :json or :edn for default serializers, a
                                custom fn can also be supplied. Only for
                                :output tasks

  ========================== Optional Settings =================================
  :kafka/request-size"
  ([job task] (add-kafka-output job task nil))
  ([job task opts]
   (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
     (-> job
         (update-in [:lifecycles] conj {:lifecycle/task task
                                        :lifecycle/calls :onyx.plugin.kafka/write-messages-calls})
         (update-in [:catalog] (fn [catalog]
                                 (replace {entry ((comp expand-serializer-fn
                                                        (partial merge opts)) entry)} catalog))))
     (throw (java.lang.IllegalArgumentException)))))


;;;;=======================================================
;;;;                     SQL
(defn add-sql-input
  "Add's sql output lifecycles to a task. Opts should include JDBC
  connection specs
  {:sql/classname ...
   :sql/subprotocol ...
   :sql/subname ...
   :sql/user ...
   :sql/password ...
   :sql/table ...}"
  ([job task] (add-sql-input job task nil))
  ([job task opts]
   (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
     (-> job
         (update-in [:lifecycles] conj {:lifecycle/task task
                                        :lifecycle/calls :onyx.plugin.sql/read-rows-calls})
         (update-in [:catalog] (fn [catalog]
                                 (replace {entry (merge opts entry)} catalog)))))))

(defn add-sql-output
  "Add's sql output lifecycles to a task. Opts should include JDBC
  connection specs
  {:sql/classname ...
   :sql/subprotocol ...
   :sql/subname ...
   :sql/user ...
   :sql/password ...
   :sql/table ...}"
  ([job task] (add-sql-output job task nil))
  ([job task opts]
   (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
     (-> job
         (update-in [:lifecycles] conj {:lifecycle/task task
                                        :lifecycle/calls :onyx.plugin.sql/write-rows-calls})
         (update-in [:catalog] (fn [catalog]
                                 (replace {entry (merge opts entry)} catalog)))))))


;;;;============================================================
;;;;                  Onyx Seq
(defn inject-in-reader [event lifecycle]
  (let [seq (:seq lifecycle)]
    {:seq/seq (read-string (slurp seq))}))

(def in-seq-calls
  {:lifecycle/before-task-start inject-in-reader})

(defn add-seq-input
  "Add's lifecycles to use an edn file as an input"
  [job task opts]
  (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
    (-> job
        (update-in [:lifecycles] into [(merge {:lifecycle/task task
                                               :lifecycle/calls ::in-seq-calls}
                                              opts)
                                       {:lifecycle/task task
                                        :lifecycle/calls :onyx.plugin.seq/reader-calls}]))))

;;;;============================================================
;;;;                    Metrics

(defn add-metrics
  "Add's throughput and latency metrics to a task"
  [job task opts]
  (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
    (-> job
        (update-in [:lifecycles] conj (merge {:lifecycle/task task ; Or :all for all tasks in the workflow
                                              :lifecycle/calls :onyx.lifecycle.metrics.metrics/calls}
                                             opts)))))


(defn build-lifecycles
  "Put your environment-independent lifecycles here"
  [ctx]
  [])
