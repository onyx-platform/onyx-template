(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]
            [com.stuartsierra.component :as component]))

(set-refresh-dirs "src" "test")

(def system nil)

(defn init [])

(defn start [])

(defn stop [])

(defn go [n-peers]
  (init n-peers)
  (start))

(defn reset []
  (stop)
  (refresh))
