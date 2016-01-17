(ns {{app-name}}.lifecycles.sample-lifecycle
    (:require [clojure.core.async :refer [chan sliding-buffer >!!]]
              [onyx.plugin.core-async :refer [take-segments!]]
              [taoensso.timbre :refer [info]]
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
(def get-input-channel
  "Returns the same channel every time for an id."
  (memoize
   (fn [id] (chan 100))))

(def get-output-channel
  "Returns the same channel every time for an id."
  (memoize
   (fn [id] (chan (sliding-buffer 100)))))

(defn inject-in-ch
  [event lifecycle]
  {:core.async/chan (get-input-channel (:core.async/id lifecycle))})

(defn inject-out-ch
  [event lifecycle]
  {:core.async/chan (get-output-channel (:core.async/id lifecycle))})

(def in-calls
  {:lifecycle/before-task-start inject-in-ch})

(def out-calls
  {:lifecycle/before-task-start inject-out-ch})

(defn get-core-async-channels [{:keys [lifecycles catalog]}]
  (let [inputs  (:onyx/name (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.core-async/input))
        outputs (:onyx/name (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.core-async/output))]
    {inputs (get-input-channel (:core.async/id
                                (first (filter #(= inputs (:lifecycle/task %)) lifecycles))))
     outputs (get-output-channel (:core.async/id
                                  (first (filter #(= outputs (:lifecycle/task %)) lifecycles))))}))

(defn add-core-async
  "Add's core.async state to corresponding catalog entries.
   Detects input/output automatically. Supports one input and one output."
  [{:keys [catalog lifecycles uuid] :as job}]
  (assert (and (sequential? catalog) (sequential? lifecycles)) "must supply a map of the form {:catalog [...] :lifecycles [...]")
  (let [inputs  (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.core-async/input)
        outputs (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.core-async/output)]
    (u/add-to-job job
                  {:lifecycles
                   (mapcat #(remove nil? %)
                           [(when-let [input-task-name (get inputs :onyx/name)]
                              [{:lifecycle/task input-task-name
                                :lifecycle/calls ::in-calls
                                :core.async/id (or uuid (java.util.UUID/randomUUID))}
                               {:lifecycle/task input-task-name
                                :lifecycle/calls :onyx.plugin.core-async/reader-calls}])
                            (when-let [output-task-name (get outputs :onyx/name)]
                              [{:lifecycle/task output-task-name
                                :lifecycle/calls ::out-calls
                                :core.async/id (or uuid (java.util.UUID/randomUUID))}
                               {:lifecycle/task output-task-name
                                :lifecycle/calls :onyx.plugin.core-async/writer-calls}])])})))

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

(def in-calls
  {:lifecycle/before-task-start inject-in-reader})

(defn add-seq
  [{:keys [catalog lifecycles] :as job} seq]
  (let [seq-input (u/find-task-by-key catalog :onyx/plugin :onyx.plugin.seq/input)]
    (-> job
        (u/add-to-job
         {:lifecycles
          [{:lifecycle/task (get seq-input :onyx/name)
            :seq seq
            :lifecycle/calls ::in-calls}
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
