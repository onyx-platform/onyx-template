(ns {{app-name}}.functions.sample-functions
  (:require [clojure.string :refer [trim capitalize]]))

(defn format-line [segment]
  (update-in segment [:line] trim))

(defn upper-case [{:keys [line] :as segment}]
  (let [upper-cased (apply str (capitalize (first line)) (rest line))]    
    (assoc-in segment [:line] upper-cased)))
