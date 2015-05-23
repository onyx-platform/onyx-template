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
            [{{app-name}}.plugins.http-reader]
            [{{app-name}}.functions.sample-functions]
            [{{app-name}}.dev-inputs.sample-input :as dev-inputs]
            [onyx.api]
            [user]))

(deftest test-sample-dev-job
  (let [dev-env (component/start (onyx-dev-env 4))]
    (try 
      (let [dev-cfg (-> "dev-peer-config.edn" resource slurp read-string)
            peer-config (assoc dev-cfg :onyx/id (:onyx-id dev-env))
            stubs [:read-lines :write-lines]
            dev-catalog (sc/in-memory-catalog (build-catalog 20) stubs)
            dev-lifecycles (sl/in-memory-lifecycles (build-lifecycles) dev-catalog stubs)]
        (sl/bind-inputs! dev-lifecycles {:read-lines dev-inputs/lines})
        (let [job {:workflow workflow
                   :catalog dev-catalog
                   :lifecycles dev-lifecycles
                   :flow-conditions sf/flow-conditions
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job)
          (let [[results] (sl/collect-outputs! dev-lifecycles [:write-lines])]
            (is (= 16 (count results))))))
      (finally 
        (component/stop dev-env)))))

(deftest test-sample-prod-job
  (let [dev-env (component/start (onyx-dev-env 4))]
    (try 
      (let [dev-cfg (-> "dev-peer-config.edn" resource slurp read-string)
            peer-config (assoc dev-cfg :onyx/id (:onyx-id dev-env))
            catalog (build-catalog 20)
            lifecycles (build-lifecycles)
            out-ch (sl/get-output-channel (sl/channel-id-for lifecycles :write-lines))]
        (let [job {:workflow workflow
                   :catalog catalog
                   :lifecycles lifecycles
                   :flow-conditions sf/flow-conditions
                   :task-scheduler :onyx.task-scheduler/balanced}]
          (onyx.api/submit-job peer-config job)
          (let [segments (onyx.plugin.core-async/take-segments! out-ch)]
            (is (= :done (last segments))))))
      (finally 
        (component/stop dev-env)))))
