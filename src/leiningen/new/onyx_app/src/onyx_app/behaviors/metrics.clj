(ns {{app-name}}.behaviors.metrics)

(defn add-metrics
  "Add's throughput and latency metrics to a task"
  [job task opts]
  (-> job
      (update-in [:lifecycles] conj (merge {:lifecycle/task task ; Or :all for all tasks in the workflow
                                            :lifecycle/calls :onyx.lifecycle.metrics.metrics/calls}
                                           opts))))
