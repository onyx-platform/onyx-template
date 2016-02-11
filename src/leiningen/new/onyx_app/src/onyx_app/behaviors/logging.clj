(ns {{app-name}}.behaviors.logging
    "Behaviors allow you to instrument tasks with special tooling for logging,
  metrics, debugging etc."
    (:require [taoensso.timbre :refer [info]]))

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
