(ns {{app-name}}.jobs.basic
  (:require [onyx.job :refer [add-task]]
            [onyx.tasks.core-async :as core-async-task]
            [{{app-name}}.tasks.math :as math]))

(defn build-job
  [batch-size batch-timeout]
  (let [batch-settings {:onyx/batch-size batch-size :onyx/batch-timeout batch-timeout}
        base-job {:workflow [[:in :inc]
                             [:inc :out]]
                  :catalog []
                  :lifecycles []
                  :windows []
                  :triggers []
                  :flow-conditions []
                  :task-scheduler :onyx.task-scheduler/balanced}]
    (-> base-job
        (add-task (core-async-task/input :in batch-settings))
        (add-task (math/inc-key :inc [:n] batch-settings))
        (add-task (core-async-task/output :out batch-settings)))))
