(ns {{app-name}}.tasks.core-async
    (:require [clojure.core.async :refer [chan sliding-buffer >!!]]
              [onyx.plugin.core-async :refer [take-segments!]]
              [taoensso.timbre :refer [info]]
              [clojure.set :refer [join]]
              [cheshire.core :as json]))

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
                                 (or (:core.async/size lifecycle) 1000))})

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
  ([job task batch-size] (add-core-async-input job task batch-size 1000 {}))
  ([job task batch-size chan-size opts]
   (-> job
       (update :catalog conj (merge {:onyx/name task
                                     :onyx/plugin :onyx.plugin.core-async/input
                                     :onyx/type :input
                                     :onyx/medium :core.async
                                     :onyx/batch-size batch-size
                                     :onyx/max-peers 1
                                     :onyx/doc "Reads segments from a core.async channel"}
                                    opts))
       (update :lifecycles into [{:lifecycle/task task
                                  :lifecycle/calls ::in-calls
                                  :core.async/id (java.util.UUID/randomUUID)
                                  :core.async/size chan-size}
                                 {:lifecycle/task task
                                  :lifecycle/calls :onyx.plugin.core-async/reader-calls}]))))

(defn add-core-async-output
  ([job task batch-size] (add-core-async-output job task batch-size 1000 {}))
  ([job task batch-size chan-size opts]
   (-> job
       (update :catalog conj (merge {:onyx/name task
                                     :onyx/plugin :onyx.plugin.core-async/output
                                     :onyx/type :output
                                     :onyx/medium :core.async
                                     :onyx/max-peers 1
                                     :onyx/batch-size batch-size
                                     :onyx/doc "Writes segments to a core.async channel"}
                                    opts))

       (update :lifecycles into [{:lifecycle/task task
                                  :core.async/id   (java.util.UUID/randomUUID)
                                  :core.async/size (inc chan-size)
                                  :lifecycle/calls ::out-calls}
                                 {:lifecycle/task task
                                  :lifecycle/calls :onyx.plugin.core-async/writer-calls}]))))
