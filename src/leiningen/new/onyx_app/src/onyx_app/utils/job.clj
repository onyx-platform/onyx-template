(ns {{app-name}}.utils.job
  (:require [onyx.schema :as os]
            [schema.core :as s]))

(s/defn add-task :- os/Job
  "Adds a task's task-definition to a job"
  [{:keys [lifecycles triggers windows flow-conditions] :as job}
   {:keys [task schema] :as task-definition}]
  (when schema (s/validate schema task))
  (cond-> job
    true (update :catalog conj (:task-map task))
    lifecycles (update :lifecycles into (:lifecycles task))
    triggers (update :triggers into (:triggers task))
    windows (update :windows into (:windows task))
    flow-conditions (update :flow-conditions into (:flow-conditions task))))

(defn add-tasks
  "Same thing as add-task, but works with sequences of tasks"
  ([job tasks]
   (reduce
    (fn [job task]
      (add-task job task)) job tasks)))
