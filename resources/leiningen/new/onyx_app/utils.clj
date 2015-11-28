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

(defn find-task-by-key
  "Finds the catalog entry where the key correspond to the value
  TODO: make this as robust as find-task"
  [catalog k value]
  (let [matches (filter #(= value (k %)) catalog)]
    (first matches)))

(defn find-task
  "Finds the catalog entry where the :onyx/name key equals task-name"
  [catalog task-name]
  (let [matches (filter #(= task-name (:onyx/name %)) catalog)]
    (when-not (seq matches)
      (throw (ex-info (format "Couldn't find task %s in catalog" task-name)
                      {:catalog catalog :task-name task-name})))
    (first matches)))

(defn update-task
  "Finds the catalog entry with :onyx/name task-name
  and applies f to it, returning the full catalog with the
  transformed catalog entry"
  [catalog task-name f]
  (mapv (fn [entry]
          (if (= task-name (:onyx/name entry))
            (f entry)
            entry))
        catalog))

(defn add-to-job
  "Adds to the catalog and lifecycles of a job in form
  {:workflow ...
   :catalog ...
   :lifecycles ...}"
  [job {:keys [catalog lifecycles]}]
  (-> job
      (update :catalog into catalog)
      (update :lifecycles into lifecycles)))

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
