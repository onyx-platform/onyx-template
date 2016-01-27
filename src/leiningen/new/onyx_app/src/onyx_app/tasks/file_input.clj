(ns {{app-name}}.tasks.file-input
    (:require [taoensso.timbre :refer [info]]))

(defn inject-in-reader [event lifecycle]
  (let [filename (:filename (:onyx.core/task-map event))]
    {:seq/seq (read-string (slurp filename))}))

(def in-seq-calls
  {:lifecycle/before-task-start inject-in-reader})

(defn add-seq-file-input
  "Adds task catalog entry and task lifecycles to use an edn file as an input"
  [job task opts]
  (-> job
      (update :catalog conj (merge {:onyx/name task
                                    :onyx/plugin :onyx.plugin.seq/input
                                    :onyx/type :input
                                    :onyx/medium :seq
                                    ;:onyx/batch-size batch-size
                                    ; :filename filename
                                    :seq/checkpoint? true
                                    :onyx/max-peers 1
                                    :onyx/doc "Reads segments from seq"}
                                   opts))
      (update :lifecycles into [{:lifecycle/task task
                                 :lifecycle/calls ::in-seq-calls}
                                {:lifecycle/task task
                                 :lifecycle/calls :onyx.plugin.seq/reader-calls}])))
