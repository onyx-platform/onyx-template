(ns {{app-name}}.jobs.sample-job-test
  (:require [clojure.core.async :refer [>!!]]
            [clojure.java.io :refer [resource]]
            [fipp.edn :refer (pprint) :rename {pprint fipp}]
            [{{app-name}}.workflows.sample-workflow :refer [workflow]]
            [{{app-name}}.catalogs.sample-catalog :refer [catalog]]
            [{{app-name}}.dev-inputs.sample-inputs :refer [dev-input-segments]]
            [{{app-name}}.lifecycles.sample-lifecycle :as sample-lifecycle]
            [{{app-name}}.functions.sample-functions]
            [onyx.plugin.core-async :refer [take-segments!]]
            [onyx.api]))

(defn test-job []
  (let [peer-config (assoc (read-string (slurp (resource "dev-peer-config.edn"))) :onyx/id (:onyx-id user/system))
        lifecycles (sample-lifecycle/build-lifecycles)
        in-ch (sample-lifecycle/get-input-channel (sample-lifecycle/channel-id-for lifecycles :read-input))
        out-ch (sample-lifecycle/get-output-channel (sample-lifecycle/channel-id-for lifecycles :write-output))]
    (doseq [segment dev-input-segments]
      (>!! in-ch segment))
    (>!! in-ch :done)
    (let [job {:workflow workflow
               :catalog catalog
               :lifecycles lifecycles
               :task-scheduler :onyx.task-scheduler/balanced}]
      (onyx.api/submit-job peer-config job)
      (fipp (take-segments! out-ch)))))
