(ns {{app-name}}.tasks.datomic
    (:require [taoensso.timbre :refer [info]]))

(defn add-log-input
  "Instrument a job with a Datomic log input lifecycles and catalog entries."
  [job task opts]
  (-> job
      (update :catalog conj (merge {:onyx/name task
                                    :onyx/plugin :onyx.plugin.datomic/read-log
                                    :onyx/type :input
                                    :onyx/medium :datomic
                                    ;; Parameters to be supplied in opts
                                    ;:onyx/batch-size 20
                                    ;:datomic/uri db-uri
                                    ;:datomic/log-start-tx <<OPTIONAL_TX_START_INDEX>>
                                    ;:datomic/log-end-tx <<OPTIONAL_TX_END_INDEX>>
                                    ;:checkpoint/force-reset? true
                                    :onyx/max-peers 1
                                    :onyx/doc "Reads a sequence of datoms from the d/log API"}
                                   opts))
      (update :lifecycles conj {:lifecycle/task task
                                :lifecycle/calls :onyx.plugin.datomic/read-log-calls})))

(defn add-writer
  "Instrument a job with a Datomic writer catalog entry and lifecycle"
  [job task opts]
  (-> job
      (update :catalog conj (merge {:onyx/name task
                                    :onyx/plugin :onyx.plugin.datomic/write-datoms
                                    :onyx/type :output
                                    :onyx/medium :datomic
                                    ;:datomic/uri db-uri
                                    ;:onyx/batch-size batch-size
                                    :onyx/doc "Transacts segments to storage"}
                              opts))
      (update :lifecycles conj {:lifecycle/task task
                                :lifecycle/calls :onyx.plugin.datomic/write-tx-calls}})))
