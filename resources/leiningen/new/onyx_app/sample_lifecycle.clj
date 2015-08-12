(ns {{app-name}}.lifecycles.sample-lifecycle
  (:require [clojure.core.async :refer [chan sliding-buffer >!!]]
            [onyx.plugin.core-async :refer [take-segments!]]
            [taoensso.timbre :refer [info]]
            [onyx.static.planning :refer [find-task]]
            [{{app-name}}.utils :as u]))

(defn log-batch [event lifecycle]
  (let [task-name (:onyx/name (:onyx.core/task-map event))]
    (doseq [m (map :message (mapcat :leaves (:tree (:onyx.core/results event))))]
      (info task-name " logging segment: " m)))
  {})

(def log-calls
  {:lifecycle/after-batch log-batch})

(defn build-lifecycles []
  [{:lifecycle/task :read-lines
    :lifecycle/calls :{{app-name}}.plugins.http-reader/reader-calls
    :lifecycle/replaceable? true
    :lifecycle/doc "Lifecycle for reading from a core.async chan"}
   {:lifecycle/task :write-lines
    :lifecycle/calls :{{app-name}}.utils/out-calls
    :core.async/id (java.util.UUID/randomUUID)
    :lifecycle/replaceable? true
    :lifecycle/doc "Lifecycle for your output task. When using in-memory-lifecycles, this will be replaced"}
   {:lifecycle/task :write-lines
    :lifecycle/calls :onyx.plugin.core-async/writer-calls
    :lifecycle/doc "Lifecycle for injecting a core.async writer chan"}
   {:lifecycle/task :write-lines
    :lifecycle/calls ::log-calls
    :lifecycle/doc "Lifecycle for printing the output of a task's batch"}])
