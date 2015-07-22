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
