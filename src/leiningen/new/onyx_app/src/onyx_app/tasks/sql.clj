(ns {{app-name}}.tasks.sql
  (:require [schema.core :as s]
            [taoensso.timbre :refer [info]]))

;; TODO, add read-rows function task

(s/defn add-sql-partition-input
  "Adds an sql patition input task to a job"
  ([job task batch-size classname subprotocol subname user password table-name id-column] 
   (add-sql-partition-input job task batch-size classname subprotocol subname user password table-name id-column {}))
  ([job task batch-size classname subprotocol subname user password table-name id-column opts]
   (-> job
       (update :catalog conj (merge {:onyx/name task
                                     :onyx/plugin :onyx.plugin.sql/partition-keys
                                     :onyx/type :input
                                     :onyx/medium :sql
                                     :sql/classname classname
                                     :sql/subprotocol subprotocol
                                     :sql/subname subname
                                     :sql/user user
                                     :sql/password password
                                     :sql/table table-name ; e.g. :your-table
                                     :sql/id id-column ; e.g. :an-id-column
                                     :sql/columns [:*]
                                     ;; 500 * 1000 = 50,000 rows
                                     ;; to be processed within :onyx/pending-timeout, 60s by default
                                     :sql/rows-per-segment 500
                                     :onyx/max-pending 1000
                                     :onyx/batch-size batch-size
                                     :onyx/max-peers 1
                                     :onyx/doc "Partitions a range of primary keys into subranges"}
                                    opts))
       (update :lifecycles conj {:lifecycle/task task
                                 :lifecycle/calls :onyx.plugin.sql/read-rows-calls}))))

(defn add-sql-insert-output
  "Adds an sql insert rows output task to a job"
  ([job task batch-size classname subprotocol subname user password table-name] 
   (add-sql-insert-output job task batch-size classname subprotocol subname user password table-name {}))
  ([job task batch-size classname subprotocol subname user password table-name opts]
   (-> job
       (update :catalog conj {:onyx/name task
                              :onyx/plugin :onyx.plugin.sql/write-rows
                              :onyx/type :output
                              :onyx/medium :sql
                              :sql/classname classname
                              :sql/subprotocol subprotocol
                              :sql/subname subname
                              :sql/user user
                              :sql/password password
                              :sql/table table-name
                              :onyx/batch-size batch-size
                              :onyx/doc "Writes segments from the :rows keys to the SQL database"})
       (update :lifecycles conj {:lifecycle/task task
                                 :lifecycle/calls :onyx.plugin.sql/write-rows-calls}))))
