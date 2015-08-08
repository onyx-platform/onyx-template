(ns {{app-name}}.jobs.sample-job-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.core.async :refer [>!!]]
            [clojure.java.io :refer [resource]]
            [com.stuartsierra.component :as component]
            [{{app-name}}.launcher.dev-system :refer [onyx-dev-env]]
            [{{app-name}}.workflows.sample-workflow :refer [workflow]]
            [{{app-name}}.catalogs.sample-catalog :refer [build-catalog] :as sc]
            [{{app-name}}.lifecycles.sample-lifecycle :refer [build-lifecycles] :as sl]
            [{{app-name}}.plugins.http-reader]
            [{{app-name}}.functions.sample-functions]
            [{{app-name}}.dev-inputs.sample-input :as dev-inputs]
            [{{app-name}}.utils :as u]
            [onyx.api]))

(deftest test-sample-dev-job
  (try
    (let [stubs [:read-lines :write-lines]
          catalog (u/in-memory-catalog (build-catalog) stubs)
          lifecycles (u/in-memory-lifecycles (build-lifecycles) catalog stubs)]
      (user/go (u/n-peers catalog workflow))
      (u/bind-inputs! lifecycles {:read-lines dev-inputs/lines})
      (let [peer-config (u/load-peer-config (:onyx-id user/system))
            job {:workflow workflow
                 :catalog catalog
                 :lifecycles lifecycles
                 :task-scheduler :onyx.task-scheduler/balanced}]
        (onyx.api/submit-job peer-config job)
        (let [[results] (u/collect-outputs! lifecycles [:write-lines])]
          (is (seq results)))))
    (catch InterruptedException e
      (Thread/interrupted))
    (finally
     (user/stop))))

(deftest test-sample-prod-job
  (try
    (let [catalog (build-catalog 20 500)
          lifecycles (build-lifecycles)]
      (user/go (u/n-peers catalog workflow))
      (u/bind-inputs! lifecycles {:read-lines dev-inputs/lines})
      (let [peer-config (u/load-peer-config (:onyx-id user/system))
            job {:workflow workflow
                 :catalog catalog
                 :lifecycles lifecycles
                 :task-scheduler :onyx.task-scheduler/balanced}]
        (onyx.api/submit-job peer-config job)
        (let [[results] (u/collect-outputs! lifecycles [:write-lines])]
          (is (seq results)))))
    (catch InterruptedException e
      (Thread/interrupted))
    (finally
     (user/stop))))
