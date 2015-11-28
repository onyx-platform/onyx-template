(ns {{app-name}}.jobs.sample-submit-job
  (:require [clojure.java.io :refer [resource]]
            [com.stuartsierra.component :as component]
            [{{app-name}}.plugins.http-reader :refer [add-http-reader]]
            [{{app-name}}.catalogs.sample-catalog :refer [build-catalog]]
            [{{app-name}}.lifecycles.sample-lifecycle :refer [build-lifecycles
                                                              add-core-async
                                                              add-logging]]
            [{{app-name}}.workflows.sample-workflow :refer [build-workflow]]
            [{{app-name}}.flow-conditions.sample-flow-conditions :refer [build-flow-conditions]]
            [{{app-name}}.dev-inputs.sample-input :as dev-inputs]
            [onyx.api]))

;;;; Lets build a job
(defn build-job [mode]
  (let [core-async?  true
        http-reader? (= mode :prod)
        logging      :write-lines
        base-job     {:catalog    (build-catalog {:batch-size 100
                                                  :batch-timeout 1000
                                                  :mode mode})
                      :lifecycles (build-lifecycles {:mode mode})
                      :workflow   (build-workflow {:mode mode})
                      :flow-conditions (build-flow-conditions {:mode mode})
                      :task-scheduler :onyx.task-scheduler/balanced}
        job          (cond-> base-job
                             core-async?  (add-core-async)
                             http-reader? (add-http-reader)
                             logging      (add-logging logging))]
    job))

(defn submit-job [])
