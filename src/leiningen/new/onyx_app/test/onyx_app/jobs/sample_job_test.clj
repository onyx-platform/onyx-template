(ns {{app-name}}.jobs.sample-job-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.core.async :refer [>!! close!]]
            [clojure.java.io :refer [resource]]
            [com.stuartsierra.component :as component]
            [onyx.test-helper :refer [load-config with-test-env]]
            [onyx.plugin.core-async :refer [take-segments!]]
            [{{app-name}}.jobs.sample-submit-job :refer [build-job]]
            [{{app-name}}.sample-input :as dev-inputs]
            [{{app-name}}.lifecycles.sample-lifecycle :refer [get-core-async-channels]]
            [onyx.api]))

(deftest onyx-dev-job-test
  (let [id (java.util.UUID/randomUUID)
        config (load-config)
        env-config (assoc (:env-config config) :onyx/id id)
        peer-config (assoc (:peer-config config) :onyx/id id)]
    (with-test-env [test-env [5 env-config peer-config]]
                   (let [job (build-job :dev)
                         {:keys [write-lines read-lines]} (get-core-async-channels job)]
                     (onyx.api/submit-job peer-config job)
                     (doseq [segment dev-inputs/lines]
                       (>!! read-lines segment))
                     (>!! read-lines :done)
                     (close! read-lines)
                     (is (= 16
                            (count (take-segments! write-lines))))))))

(deftest onyx-prod-job-test
  (let [id (java.util.UUID/randomUUID)
        config (load-config)
        env-config (assoc (:env-config config) :onyx/id id)
        peer-config (assoc (:peer-config config) :onyx/id id)]
    (with-test-env [test-env [5 env-config peer-config]]
                   (let [job (build-job :prod)
                         {:keys [write-lines read-lines]} (get-core-async-channels job)]
                     (onyx.api/submit-job peer-config job)
                     (is (take-segments! write-lines))))))
