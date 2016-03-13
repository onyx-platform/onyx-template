(ns {{app-name}}.jobs.meetup-job-test
    (:require [clojure.test :refer [deftest is use-fixtures]]
              [{{app-name}}.jobs.meetup-job :refer [build-job]]
              [{{app-name}}.tasks.core-async :refer [get-core-async-channels]]
              [onyx api
               [test-helper :refer [feedback-exception! load-config validate-enough-peers! with-test-env]]]
              [onyx.plugin.core-async :refer [take-segments!]]
              [onyx.plugin.seq]
              [schema.test]
              {{#metrics?}}[onyx.lifecycle.metrics.metrics]{{/metrics?}}
              {{#metrics?}}[onyx.lifecycle.metrics.timbre]{{/metrics?}}))

(use-fixtures :once schema.test/validate-schemas)

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
