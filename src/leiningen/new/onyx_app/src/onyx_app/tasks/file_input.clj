(ns {{app-name}}.tasks.file-input
    (:require [taoensso.timbre :refer [info]]
              [schema.core :as s]
              [onyx.schema :as os]))

(defn inject-in-reader [event lifecycle]
  (let [filename (:filename (:onyx.core/task-map event))]
    {:seq/seq (read-string (slurp filename))}))

(def in-seq-calls
  {:lifecycle/before-task-start inject-in-reader})

(s/defschema SeqInputTask
  {(s/required-key :filename) s/Str
   (s/optional-key :seq/checkpoint?) s/Bool})

(s/defn input-task
  [task-name :- s/Keyword opts :- SeqInputTask]
  {:task {:task-map (merge {:onyx/name task-name
                            :onyx/plugin :onyx.plugin.seq/input
                            :onyx/type :input
                            :onyx/medium :seq
                            :onyx/max-peers 1
                            :onyx/doc "Reads segments from seq"}
                           opts)
          :lifecycles [{:lifecycle/task task-name
                        :lifecycle/calls ::in-seq-calls}
                       {:lifecycle/task task-name
                        :lifecycle/calls :onyx.plugin.seq/reader-calls}]}
   :schema {:task-map (merge os/TaskMap SeqInputTask)
            :lifecycles [os/Lifecycle]}})
