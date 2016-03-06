(ns {{app-name}}.tasks.sql
  (:require [schema.core :as s]
            [onyx.schema :as os]))

;; TODO, add read-rows function task

(s/defschema SqlPartitionInput
  {(s/required-key :sql/classname) s/Str
   (s/required-key :sql/subprotocol) s/Str
   (s/required-key :sql/subname) s/Str
   (s/required-key :sql/user) s/Str
   (s/required-key :sql/password) s/Str
   (s/required-key :sql/table) s/Str

   (s/required-key :sql/id) s/Str
   (s/optional-key :sql/columns) [s/Any]
   (s/optional-key :sql/rows-per-segment) s/Num})

(s/defschema SqlInsertOutput
  {(s/required-key :sql/classname) s/Str
   (s/required-key :sql/subprotocol) s/Str
   (s/required-key :sql/subname) s/Str
   (s/required-key :sql/user) s/Str
   (s/required-key :sql/password) s/Str
   (s/required-key :sql/table) s/Str})

(s/defn partition-input
  [task-name :- s/Keyword opts :- SqlPartitionInput]
  {:task {:task-map (merge
                      {:onyx/name task-name
                       :onyx/plugin :onyx.plugin.sql/partition-keys
                       :onyx/type :input
                       :onyx/medium :sql
                       :sql/columns [:*]
                       :sql/rows-per-segment 500
                       :onyx/max-pending 1000
                       :onyx/max-peers 1
                       :onyx/doc "Partitions a range of primary keys into subranges"}
                      opts)
          :lifecycles [{:lifecycle/task task-name
                        :lifecycle/calls :onyx.plugin.sql/read-rows-calls}]}
   :schema {:task-map (merge os/TaskMap SqlPartitionInput)
            :lifecycles [os/Lifecycle]}})

(s/defn insert-output
  [task-name :- s/Keyword opts :- SqlInsertOutput]
  {:task {:task-map (merge
                     {:onyx/name task-name
                      :onyx/plugin :onyx.plugin.sql/write-rows
                      :onyx/type :output
                      :onyx/medium :sql
                                        ;:onyx/batch-size batch-size
                                        ;:sql/classname classname
                                        ;:sql/subprotocol subprotocol
                                        ;:sql/subname subname
                                        ;:sql/user user
                                        ;:sql/password password
                                        ;:sql/table table-name
                      :onyx/doc "Writes segments from the :rows keys to the SQL database"}
                     opts)
          :lifecycles [{:lifecycle/task task-name
                        :lifecycle/calls :onyx.plugin.sql/write-rows-calls}]}
   :schema {:task-map (merge os/TaskMap SqlInsertOutput)
            :lifecycles [os/Lifecycle]}})
