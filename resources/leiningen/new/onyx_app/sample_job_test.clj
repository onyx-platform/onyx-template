(ns {{app-name}}.jobs.sample-job-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.core.async :refer [>!!]]
            [clojure.java.io :refer [resource]]
            [com.stuartsierra.component :as component]
            [{{app-name}}.launcher.dev-system :refer [onyx-dev-env]]
            [{{app-name}}.workflows.sample-workflow :refer [workflow]]
            [{{app-name}}.catalogs.sample-catalog :refer [build-catalog] :as sc]
            [{{app-name}}.lifecycles.sample-lifecycle :refer [build-lifecycles] :as sl]
            [{{app-name}}.flow-conditions.sample-flow-conditions :as sf]
            [{{app-name}}.jobs.sample-submit-job :as submit-job]
            [{{app-name}}.plugins.http-reader]
            [{{app-name}}.functions.sample-functions]
            [{{app-name}}.dev-inputs.sample-input :as dev-inputs]
            [onyx.api]
            [user]))

(deftest test-sample-dev-job
  ;; Start the development environment with 4 peers.
  (let [dev-env (component/start (onyx-dev-env 4))]
    (try 
      (let [{:keys [write-lines]} (submit-job/submit-job dev-env)]
        (is (= 16 (count write-lines)))))
      (finally 
        (component/stop dev-env)))))

(deftest test-sample-prod-job
  (let [dev-env (component/start (onyx-dev-env 4))]
    (try 
      (let [dev-cfg (-> "dev-peer-config.edn" resource slurp read-string)
            peer-config (assoc dev-cfg :onyx/id (:onyx-id dev-env))
            catalog (build-catalog 20 50)
            lifecycles (build-lifecycles)
            ;; Uses a utility function to locate the channel for output by
            ;; its lifecycle ID.
            out-ch (sl/get-output-channel (sl/channel-id-for lifecycles :write-lines))]
        (let [job {:workflow workflow
                   :catalog catalog
                   :lifecycles lifecycles
                   :flow-conditions sf/flow-conditions
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job)
          ;; Uses a utility function in the core.async plugin to read
          ;; all segments until :done is found, then stops reading.
          (let [segments (onyx.plugin.core-async/take-segments! out-ch)]
            (is (= :done (last segments))))))
      (finally 
        (component/stop dev-env)))))
