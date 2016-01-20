(ns {{app-name}}.lifecycles.sample-lifecycle
    (:require [clojure.core.async :refer [chan sliding-buffer >!!]]
              [onyx.plugin.core-async :refer [take-segments!]]
              [taoensso.timbre :refer [info]]
              [clojure.set :refer [join]]
              [{{app-name}}.utils :as u]
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
  (assert (and (keyword? task) (u/find-task (:catalog job) task)))
  (u/add-to-job job {:lifecycles [{:lifecycle/task task
                                   :lifecycle/calls ::log-calls
                                   :lifecycle/doc "Lifecycle for printing the output of a task's batch"}]}))

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
(defn deserialize-message [bytes]
  (try
    (json/parse-string (String. bytes "UTF-8"))
    (catch Exception e
      {:error e})))

(defn add-kafka
  [{:keys [catalog lifecycles] :as job} kafka-settings]
  "Instruments a :task with Kafka lifecycle and catalog settings.
   Must supply :kafka/topic, :kafka/partition, :kafka/group-id and :kafka/zookeeper"
  (assert (not (some nil? ((juxt :kafka/topic
                                 :kafka/group-id :kafka/zookeeper)
                           kafka-settings)))
          "Need to specify :kafka/topic, :kafka/group-id and :kafka/zookeeper")
  (let [kafka-settings (merge {:kafka/deserializer-fn ::deserialize-message
                               :kafka/offset-reset :smallest
                               :kafka/force-reset? true}
                              kafka-settings)
        kafka-input    (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.kafka/read-messages)
        kafka-output   (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.kafka/write-messages)]
    (-> job
        (update-in [:catalog] (fn [entries]
                                (mapv (fn [entry]
                                        (if (= (get entry :onyx/plugin)
                                               :onyx.plugin.kafka/read-messages)
                                          (merge entry kafka-settings)
                                          entry))
                                      entries)))
        (u/add-to-job
         {:lifecycles
          (mapcat #(remove nil? %)
                  [(when-let [task-name (get kafka-input :onyx/name)]
                     [{:lifecycle/task task-name
                       :lifecycle/calls :onyx.plugin.kafka/read-messages-calls}])])}))))

;;;;=======================================================
;;;;                     SQL
(defn add-sql
  [{:keys [catalog lifecycles] :as job}]
  (let [sql-input (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.sql/read-rows)
        sql-output (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.sql/write-rows)]
    (-> job
        (u/add-to-job
         {:lifecycles
          (mapcat #(remove nil? %)
                  [(when-let [task-name (get sql-output :onyx/name)]
                     [{:lifecycle/task task-name
                       :lifecycle/calls :onyx.plugin.sql/write-rows-calls}])])}))))

(defn build-lifecycles
  "Put your environment-independent lifecycles here"
  [ctx]
  [])

;;;;============================================================
;;;;                  Onyx Seq
(defn inject-in-reader [event lifecycle]
  (let [seq (:seq lifecycle)]
    {:seq/seq seq}))

(def in-seq-calls
  {:lifecycle/before-task-start inject-in-reader})

(defn add-seq
  [{:keys [catalog lifecycles] :as job} seq]
  (let [seq-input (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.seq/input)]
    (-> job
        (u/add-to-job
         {:lifecycles
          [{:lifecycle/task (get seq-input :onyx/name)
            :seq seq
            :lifecycle/calls ::in-seq-calls}
           {:lifecycle/task (get seq-input :onyx/name)
            :lifecycle/calls :onyx.plugin.seq/reader-calls}]}))))


;;;;============================================================
;;;;                    Metrics
(defn add-metrics
  [job task]
  (u/add-to-job job
   {:lifecycles
    [{:lifecycle/task task ; Or :all for all tasks in the workflow
      :lifecycle/calls :onyx.lifecycle.metrics.metrics/calls
      :metrics/buffer-capacity 10000
      :metrics/workflow-name "meetup-workflow"
      :metrics/sender-fn :onyx.lifecycle.metrics.timbre/timbre-sender
      :lifecycle/doc "Instruments a task's metrics to timbre"}]}))
