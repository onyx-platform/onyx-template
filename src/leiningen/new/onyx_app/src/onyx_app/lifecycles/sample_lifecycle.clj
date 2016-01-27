(ns {{app-name}}.lifecycles.sample-lifecycle
    (:require [clojure.core.async :refer [chan sliding-buffer >!!]]
              [onyx.plugin.core-async :refer [take-segments!]]
              [taoensso.timbre :refer [info]]
              [clojure.set :refer [join]]))

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

;;;;============================================================
;;;;                    Metrics

(defn add-metrics
  "Add's throughput and latency metrics to a task"
  [job task opts]
  (-> job
      (update-in [:lifecycles] conj (merge {:lifecycle/task task ; Or :all for all tasks in the workflow
                                            :lifecycle/calls :onyx.lifecycle.metrics.metrics/calls}
                                           opts))))

(defn build-lifecycles
  "Put your environment-independent lifecycles here"
  [ctx]
  [])
