(ns {{app-name}}.utils
    (:require [clojure.test :refer [is]]
              [clojure.core.async :refer [chan sliding-buffer >!!]]
              [clojure.java.io :refer [resource]]
              [onyx.plugin.core-async :refer [take-segments!]]))

;;;; Test utils ;;;;

(def zk-address "127.0.0.1")

(def zk-port 2188)

(def zk-str (str zk-address ":" zk-port))

(defn only [coll]
  (assert (not (next coll)))
  (if-let [result (first coll)]
    result
    (assert false)))

(defn find-task [catalog task-name]
  (let [matches (filter #(= task-name (:onyx/name %)) catalog)]
    (when-not (seq matches)
      (throw (ex-info (format "Couldn't find task %s in catalog" task-name)
                      {:catalog catalog :task-name task-name})))
    (first matches)))

(defn n-peers
  "Takes a workflow and catalog, returns the minimum number of peers
   needed to execute this job."
  [catalog workflow]
  (let [task-set (into #{} (apply concat workflow))]
    (reduce
     (fn [sum t]
       (+ sum (or (:onyx/min-peers (find-task catalog t)) 1)))
     0 task-set)))

(defn segments-equal?
  "Onyx is a parallel, distributed system - so ordering isn't guaranteed.
   Does an unordered comparison of segments to check for equality."
  [expected actual]
  (is (= (into #{} expected) (into #{} (remove (partial = :done) actual))))
  (is (= :done (last actual)))
  (is (= (dec (count actual)) (count expected))))

(defn load-peer-config [onyx-id]
  (assoc (-> "dev-peer-config.edn" resource slurp read-string)
    :onyx/id onyx-id
    :zookeeper/address zk-str))

(defn load-env-config [onyx-id]
  (assoc (-> "env-config.edn" resource slurp read-string)
    :onyx/id onyx-id
    :zookeeper/address zk-str
    :zookeeper.server/port zk-port))

(defn in-memory-catalog
  "Takes a catalog and a set of input/output task names,
   returning a new catalog with all I/O catalog entries
   that were specified turned into core.async plugins. The
   core.async entries preserve all non-Onyx parameters."
  [catalog tasks]
  (mapv
   (fn [entry]
     (cond (and (some #{(:onyx/name entry)} tasks) (= (:onyx/type entry) :input))
           (merge
            entry
            {:onyx/plugin :onyx.plugin.core-async/input
             :onyx/type :input
             :onyx/medium :core.async
             :onyx/max-peers 1
             :onyx/doc "Reads segments from a core.async channel"})
           (and (some #{(:onyx/name entry)} tasks) (= (:onyx/type entry) :output))
           (merge
            entry
            {:onyx/plugin :onyx.plugin.core-async/output
             :onyx/type :output
             :onyx/medium :core.async
             :onyx/max-peers 1
             :onyx/doc "Writes segments to a core.async channel"})
           :else entry))
   catalog))

;;;; Lifecycles utils ;;;;

(def input-channel-capacity 10000)

(def output-channel-capacity (inc input-channel-capacity))

(def get-input-channel
  (memoize
   (fn [id] (chan input-channel-capacity))))

(def get-output-channel
  (memoize
   (fn [id] (chan (sliding-buffer output-channel-capacity)))))

(defn channel-id-for [lifecycles task-name]
  (->> lifecycles
       (filter #(= task-name (:lifecycle/task %)))
       (map :core.async/id)
       (remove nil?)
       (first)))

(defn inject-in-ch [event lifecycle]
  {:core.async/chan (get-input-channel (:core.async/id lifecycle))})

(defn inject-out-ch [event lifecycle]
  {:core.async/chan (get-output-channel (:core.async/id lifecycle))})

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
      (let [catalog-entry (find-task catalog task)]
        (cond (and (some #{task} tasks) replaceable?
                   (= (:onyx/type catalog-entry) :input))
              [{:lifecycle/task task
                :lifecycle/calls ::in-calls
                :core.async/id (java.util.UUID/randomUUID)}
               {:lifecycle/task task
                :lifecycle/calls :onyx.plugin.core-async/reader-calls}]
              (and (some #{task} tasks) replaceable?
                   (= (:onyx/type catalog-entry) :output))
              [{:lifecycle/task task
                :lifecycle/calls ::out-calls
                :core.async/id (java.util.UUID/randomUUID)}
               {:lifecycle/task task
                :lifecycle/calls :onyx.plugin.core-async/writer-calls}]
              :else [lifecycle])))
    lifecycles)))


(defn bind-inputs! [lifecycles mapping]
  (doseq [[task segments] mapping]
    (let [in-ch (get-input-channel (channel-id-for lifecycles task))
          n-segments (count segments)]
      (when (< input-channel-capacity n-segments)
        (throw (ex-info "Input channel capacity is smaller than bound inputs. Capacity can be adjusted in utils.clj"
                        {:channel-size input-channel-capacity
                         :n-segments n-segments})))
      (when-not ((set (map :lifecycle/task lifecycles)) task)
        (throw (ex-info (str "Cannot bind input for task " task " as lifecycles are missing. Check that inputs are being bound to the correct task name.")
                        {:input task
                         :lifecycles lifecycles})))
      (doseq [segment segments]
        (>!! in-ch segment))
      (>!! in-ch :done))))

(defn collect-outputs! [lifecycles output-tasks]
  (->> output-tasks
       (map #(get-output-channel (channel-id-for lifecycles %)))
       (map #(take-segments! %))))
