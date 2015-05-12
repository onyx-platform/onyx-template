(ns {{app-name}}.jobs.sample-job-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.core.async :refer [>!!]]
            [clojure.java.io :refer [resource]]
            [{{app-name}}.workflows.sample-workflow :refer [workflow]]
            [{{app-name}}.catalogs.sample-catalog :refer [catalog]]
            [{{app-name}}.lifecycles.sample-lifecycle :as lc]
            [{{app-name}}.dev-inputs.sample-input :as dev-inputs]
            [{{app-name}}.functions.sample-functions]
            [onyx.api]
            [user]))

(deftest test-sample-job
  (let [dev-cfg (-> "dev-peer-config.edn" resource slurp read-string)
        peer-config (assoc dev-cfg :onyx/id (:onyx-id user/system))
        lifecycles (lc/build-lifecycles)]
    (lc/bind-inputs! lifecycles {:read-input dev-inputs/name-segments})
    (let [job {:workflow workflow
               :catalog catalog
               :lifecycles lifecycles
               :task-scheduler :onyx.task-scheduler/balanced}]
      (onyx.api/submit-job peer-config job)
      (let [[results] (lc/collect-outputs! lifecycles [:write-output])]
        (is (= 31 (count results)))))))
