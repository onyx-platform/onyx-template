(ns {{app-name}}.launcher.submit-prod-sample-job
  (:require [clojure.java.io :refer [resource]]
            [{{app-name}}.workflows.sample-workflow :refer [workflow]]
            [{{app-name}}.catalogs.sample-catalog :refer [catalog]]
            [{{app-name}}.lifecycles.sample-lifecycle :as sample-lifecycle]
            [{{app-name}}.functions.sample-functions]
            [onyx.plugin.core-async :refer [take-segments!]]
            [onyx.api]))

(defn -main [onyx-id & args]
  (let [peer-config (assoc (-> "prod-peer-config.edn"
                               resource slurp read-string) :onyx/id onyx-id)
        ;; TODO: Transfer dev catalog entries and lifecycles into prod
        ;; IO streams.
        lifecycles (sample-lifecycle/build-lifecycles)]
    (let [job {:workflow workflow
               :catalog catalog
               :lifecycles lifecycles
               :task-scheduler :onyx.task-scheduler/balanced}]
      (onyx.api/submit-job peer-config job))))
