(ns {{app-name}}.jobs.sample-job-test
    (:require [clojure.test :refer [deftest is]]
      [onyx.test-helper :refer [load-config with-test-env]]
      [onyx.plugin.core-async :refer [take-segments!]]
      [{{app-name}}.jobs.sample-submit-job :refer [build-job]]
      [{{app-name}}.lifecycles.sample-lifecycle :refer [get-core-async-channels]]
      [{{app-name}}.launcher.launch-prod-peers]            ;; Load namespace to include runtime deps
      [onyx.api]))

(deftest onyx-dev-job-test
         (let [id (java.util.UUID/randomUUID)
               config (load-config)
               env-config (assoc (:env-config config) :onyx/id id)
               peer-config (assoc (:peer-config config) :onyx/id id)]
           ;; Be sure to set the peer count (5 here) to a number greater than
           ;; the amount of tasks in your job.
              (with-test-env [test-env [5 env-config peer-config]]
                             (let [job (build-job :dev)
                                   {:keys [write-lines]} (get-core-async-channels job)]
                                  (onyx.api/submit-job peer-config job)
                                  (let [results (take-segments! write-lines)]
                                       (is (= 4 (count results)))
                                       (is (= :done (last results))))))))