(ns {{app-name}}.lifecycles.sample-lifecycle
  (:require [clojure.core.async :refer [chan sliding-buffer]]))

(def input-channel-capacity 10000)

(def output-channel-capacity (inc input-channel-capacity))

(def get-input-channel
  (memoize
   (fn [id]
     (chan input-channel-capacity))))

(def get-output-channel
  (memoize
   (fn [id]
     (chan (sliding-buffer output-channel-capacity)))))

(defn channel-id-for [lifecycles task-name]
  (:core.async/id (first (filter #(= task-name (:lifecycle/task %)) lifecycles))))

(defn inject-in-ch [event lifecycle]
  {:core.async/chan (get-input-channel (:core.async/id lifecycle))})

(defn inject-out-ch [event lifecycle]
  {:core.async/chan (get-output-channel (:core.async/id lifecycle))})

(def in-calls
  {:lifecycle/before-task inject-in-ch})

(def out-calls
  {:lifecycle/before-task inject-out-ch})

(defn build-lifecycles []
  [{:lifecycle/task :read-input
    :lifecycle/calls :{{app-name}}.lifecycles.sample-lifecycle/in-calls
    :core.async/id (java.util.UUID/randomUUID)}
   {:lifecycle/task :read-input
    :lifecycle/calls :onyx.plugin.core-async/reader-calls}
   {:lifecycle/task :write-output
    :lifecycle/calls :{{app-name}}.lifecycles.sample-lifecycle/out-calls
    :core.async/id (java.util.UUID/randomUUID)}
   {:lifecycle/task :write-output
    :lifecycle/calls :onyx.plugin.core-async/writer-calls}])
