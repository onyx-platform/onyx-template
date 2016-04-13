(ns {{app-name}}.tasks.metrics)

(defn add-timbre-metrics
  "Adds throughput and latency metrics to a task"
  ([job task] (add-timbre-metrics job task {}))
  ([job task opts]
   (-> job
       (update-in [:lifecycles] conj (merge {:lifecycle/task task ; Or :all for all tasks in the workflow
                                             :metrics/buffer-capacity 10000
                                             :metrics/sender-fn :onyx.lifecycle.metrics.timbre/timbre-sender
                                             :lifecycle/doc "Instruments task metrics to timbre"
                                             :lifecycle/calls :onyx.lifecycle.metrics.metrics/calls}
                                            opts)))))
