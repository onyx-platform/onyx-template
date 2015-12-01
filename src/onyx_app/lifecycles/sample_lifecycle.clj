(ns onyx-app.lifecycles.sample-lifecycle
  (:require [clojure.core.async :refer [chan sliding-buffer >!!]]
            [onyx.plugin.core-async :refer [take-segments!]]
            [taoensso.timbre :refer [info]]
            [onyx.static.planning :refer [find-task]]
            [onyx-app.utils :as u]))


;;;;================- Logging -===========================

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

;;;;================- Core.Async  -===========================
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
  (let [inputs  (u/find-task-by-key catalog :onyx/plugin
                                    :onyx.plugin.core-async/input)
        outputs (u/find-task-by-key catalog :onyx/plugin
                                    :onyx.plugin.core-async/output)]
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

(defn build-lifecycles
  "Put your environment-independent lifecycles here"
  [ctx]
  [])
