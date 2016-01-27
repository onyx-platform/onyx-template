(ns {{app-name}}.jobs.sample-job-test
    (:require [clojure.test :refer [deftest is]]
              [onyx api
              [test-helper :refer [feedback-exception! validate-enough-peers! load-config with-test-env]]]
              [{{app-name}}.jobs.sample-submit-job :refer [build-job]]
              [{{app-name}}.lifecycles.sample-lifecycle :refer [get-core-async-channels]]
              {{#metrics?}}[onyx.lifecycle.metrics.metrics]{{/metrics?}}
              {{#metrics?}}[onyx.lifecycle.metrics.timbre]{{/metrics?}}
              [onyx.plugin.core-async :refer [take-segments!]]
              ; Make the plugins load
              [onyx.plugin.kafka]
              [onyx.plugin.seq]
              [onyx.plugin.sql]))

(deftest onyx-dev-job-test
  (let [id (java.util.UUID/randomUUID)
        config (load-config)
        env-config (assoc (:env-config config) :onyx/id id)
        peer-config (assoc (:peer-config config) :onyx/id id)]
    ;; Be sure to set the peer count (5 here) to a number greater than
    ;; the amount of tasks in your job.
    (with-test-env [test-env [5 env-config peer-config]]
      (let [job (build-job :dev)
            {:keys [write-lines]} (get-core-async-channels job)
            _ (validate-enough-peers! test-env job)
            {:keys [job-id]} (onyx.api/submit-job peer-config job)]
        (feedback-exception! peer-config job-id)
        (let [results (take-segments! write-lines)]
          (is (= 4 (count results)))
          (is (= :done (last results))))))))
