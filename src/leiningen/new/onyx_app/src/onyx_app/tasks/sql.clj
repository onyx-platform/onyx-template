(ns {{app-name}}.tasks.sql
    (:require [taoensso.timbre :refer [info]]))

(defn add-sql-input
  "Add's sql output lifecycles to a task. Opts should include JDBC
  connection specs
  {:sql/classname ...
   :sql/subprotocol ...
   :sql/subname ...
   :sql/user ...
   :sql/password ...
   :sql/table ...}"
  ([job task] (add-sql-input job task nil))
  ([job task opts]
   (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
     (-> job
         (update-in [:lifecycles] conj {:lifecycle/task task
                                        :lifecycle/calls :onyx.plugin.sql/read-rows-calls})
         (update-in [:catalog] (fn [catalog]
                                 (replace {entry (merge opts entry)} catalog)))))))

(defn add-sql-output
  "Add's sql output lifecycles to a task. Opts should include JDBC
  connection specs
  {:sql/classname ...
   :sql/subprotocol ...
   :sql/subname ...
   :sql/user ...
   :sql/password ...
   :sql/table ...}"
  ([job task] (add-sql-output job task nil))
  ([job task opts]
   (if-let [entry (first (filter #(= (:onyx/name %) task) (:catalog job)))]
     (-> job
         (update-in [:lifecycles] conj {:lifecycle/task task
                                        :lifecycle/calls :onyx.plugin.sql/write-rows-calls})
         (update-in [:catalog] (fn [catalog]
                                 (replace {entry (merge opts entry)} catalog)))))))
