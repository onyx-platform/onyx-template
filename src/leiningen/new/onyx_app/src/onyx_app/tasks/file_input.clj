(ns {{app-name}}.tasks.file-input
    (:require [taoensso.timbre :refer [info]]))

(defn inject-in-reader [event lifecycle]
  (let [seq (:seq lifecycle)]
    {:seq/seq (read-string (slurp seq))}))

(def in-seq-calls
  {:lifecycle/before-task-start inject-in-reader})

(defn add-seq-file-input
  "Adds task catalog entry and task lifecycles to use an edn file as an input"
  ([job task batch-size filename]
   (add-seq-file-input job task batch-size filename {}))
  ([job task batch-size filename opts]
   (-> job
       (update :catalog conj (merge {:onyx/name task
                                     :onyx/plugin :onyx.plugin.seq/input
                                     :onyx/type :input
                                     :onyx/medium :seq
                                     :seq/checkpoint? true
                                     :onyx/batch-size batch-size
                                     :onyx/max-peers 1
                                     :onyx/doc "Reads segments from seq"}
                                    opts))
       (update :lifecycles into [{:lifecycle/task task
                                  :seq filename
                                  :lifecycle/calls ::in-seq-calls}
                                 {:lifecycle/task task
                                  :lifecycle/calls :onyx.plugin.seq/reader-calls}]))))
