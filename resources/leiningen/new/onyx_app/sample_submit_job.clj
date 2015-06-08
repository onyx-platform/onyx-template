(ns {{app-name}}.jobs.sample-submit-job
  (:require [clojure.java.io :refer [resource]]
            [com.stuartsierra.component :as component]
            [{{app-name}}.workflows.sample-workflow :refer [workflow]]
            [{{app-name}}.catalogs.sample-catalog :refer [build-catalog] :as sc]
            [{{app-name}}.lifecycles.sample-lifecycle :refer [build-lifecycles] :as sl]
            [{{app-name}}.flow-conditions.sample-flow-conditions :as sf]
            [{{app-name}}.functions.sample-functions]
            [{{app-name}}.dev-inputs.sample-input :as dev-inputs]
            [onyx.api]))

(defn submit-job [dev-env]
  (let [dev-cfg (-> "dev-peer-config.edn" resource slurp read-string)
        peer-config (assoc dev-cfg :onyx/id (:onyx-id dev-env))
        ;; Turn :read-lines and :write-lines into core.async I/O channels
        stubs [:read-lines :write-lines]
        ;; Stubs the catalog entries for core.async I/O
        dev-catalog (sc/in-memory-catalog (build-catalog 20 50) stubs)
        ;; Stubs the lifecycles for core.async I/O
        dev-lifecycles (sl/in-memory-lifecycles (build-lifecycles) dev-catalog stubs)]
    ;; Automatically pipes the data structure into the channel, attaching :done at the end
    (sl/bind-inputs! dev-lifecycles {:read-lines dev-inputs/lines})
    (let [job {:workflow workflow
               :catalog dev-catalog
               :lifecycles dev-lifecycles
               :flow-conditions sf/flow-conditions
               :task-scheduler :onyx.task-scheduler/balanced}]
      (onyx.api/submit-job peer-config job)
      ;; Automatically grab output from the stubbed core.async channels,
      ;; returning a vector of the results with data structures representing
      ;; the output.
      (sl/collect-outputs! dev-lifecycles [:write-lines]))))
      
