(ns {{app-name}}.functions.sample-functions
  (:require [clojure.string :refer [trim capitalize]]))

;;; Defines functions to be used by the peers. These are located
;;; with fully qualified namespaced keywords, such as
;;; {{app-name}}.functions.sample-functions/format-line

(defn format-line [segment]
  (update-in segment [:line] trim))

(defn upper-case [{:keys [line] :as segment}]
  (if (seq line)
    (let [upper-cased (apply str (capitalize (first line)) (rest line))]
      (assoc-in segment [:line] upper-cased))
    segment))

(defn transform-segment-shape
  "Recursively restructures a segment {:new-key [paths...]}"
  [paths segment]
  (try (let [f (fn [[k v]]
                 (if (vector? v)
                   [k (get-in segment v)]
                   [k v]))]
         (postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) paths))
       (catch Exception e
         segment)))

(defn get-in-segment [keypath segment]
  (get-in segment keypath))

(defn prepare-rows [segment]
  {:rows [segment]})
