(ns {{app-name}}.lifecycles.sample-lifecycle
  (:require [clojure.core.async :refer [chan sliding-buffer >!!]]
            [onyx.plugin.core-async :refer [take-segments!]]
            [onyx.static.planning :refer [find-task]]
            [{{app-name}}.utils :as u]))

(defn inject-in-ch [event lifecycle]
  {:core.async/chan (u/get-input-channel (:core.async/id lifecycle))})

(defn inject-out-ch [event lifecycle]
  {:core.async/chan (u/get-output-channel (:core.async/id lifecycle))})

(def in-calls
  {:lifecycle/before-task-start inject-in-ch})

(def out-calls
  {:lifecycle/before-task-start inject-out-ch})

;;; Stubs lifecycles to use core.async IO, instead of, say, Kafka or Datomic.
(defn in-memory-lifecycles
  [lifecycles catalog tasks]
  (vec
   (mapcat
    (fn [{:keys [lifecycle/task lifecycle/replaceable?] :as lifecycle}]
      (let [catalog-entry (u/find-task catalog task)]
        (cond (and (some #{task} tasks) replaceable?
                   (= (:onyx/type catalog-entry) :input))
              [{:lifecycle/task task
                :lifecycle/calls :{{app-name}}.lifecycles.sample-lifecycle/in-calls
                :core.async/id (java.util.UUID/randomUUID)}
               {:lifecycle/task task
                :lifecycle/calls :onyx.plugin.core-async/reader-calls}]
              (and (some #{task} tasks) replaceable?
                   (= (:onyx/type catalog-entry) :output))
              [{:lifecycle/task task
                :lifecycle/calls :{{app-name}}.lifecycles.sample-lifecycle/out-calls
                :core.async/id (java.util.UUID/randomUUID)}
               {:lifecycle/task task
                :lifecycle/calls :onyx.plugin.core-async/writer-calls}]
              :else [lifecycle])))
    lifecycles)))

;;;; Lifecycle hook to debug segments by logging them to the console.
(defn log-batch [event lifecycle]
  (doseq [m (map :message (mapcat :leaves (:tree (:onyx.core/results event))))]
    (prn "Logging segment: " m))
  {})

(def log-calls
  {:lifecycle/after-batch log-batch})

(defn build-lifecycles []
  [{:lifecycle/task :read-lines
    :lifecycle/calls :{{app-name}}.plugins.http-reader/reader-calls
    :lifecycle/replaceable? true
    :lifecycle/doc "Lifecycle for reading from a core.async chan"}
   {:lifecycle/task :write-lines
    :lifecycle/calls :{{app-name}}.lifecycles.sample-lifecycle/out-calls
    :core.async/id (java.util.UUID/randomUUID)
    :lifecycle/doc "Lifecycle for writing to a core.async chan"}
   {:lifecycle/task :write-lines
    :lifecycle/calls :onyx.plugin.core-async/writer-calls
    :lifecycle/doc "Lifecycle for injecting a core.async writer chan"}
   {:lifecycle/task :write-lines
    :lifecycle/calls :{{app-name}}.lifecycles.sample-lifecycle/log-calls
    :lifecycle/doc "Lifecycle for printing the output of a task's batch"}])
