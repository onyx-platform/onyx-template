(ns {{app-name}}.launcher.launch-prod-peers
  (:require [clojure.core.async :refer [<!! chan]]
            [aero.core :refer [read-config]]
            {{#docker?}}[taoensso.timbre :as t]{{/docker?}}
            [onyx.plugin.kafka]
            [onyx.plugin.sql]
            [onyx.plugin.core-async]
            [onyx.plugin.seq]
            {{#metrics?}}[onyx.lifecycle.metrics.timbre]{{/metrics?}}
            {{#metrics?}}[onyx.lifecycle.metrics.metrics]{{/metrics?}}
            [{{app-name}}.functions.sample-functions]
            [{{app-name}}.jobs.sample-submit-job]
            [{{app-name}}.lifecycles.sample-lifecycle])
  (:gen-class))

(defn -main [n & args]
  (let [n-peers (Integer/parseInt n)
        config (read-config (clojure.java.io/resource "config.edn") {:profile :default})
        peer-config (-> (:peer-config config)
                        {{#docker?}}(assoc :onyx.log/config {:appenders {} :min-level :info}){{/docker?}})
        peer-group (onyx.api/start-peer-group peer-config)
        peers (onyx.api/start-peers n-peers peer-group)]
    (println "Attempting to connec to to Zookeeper: " (:zookeeper/address peer-config))
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread.
                        (fn []
                          (doseq [v-peer peers]
                            (onyx.api/shutdown-peer v-peer))
                          (onyx.api/shutdown-peer-group peer-group)
                          (shutdown-agents))))
    (println "Started peers. Blocking forever.")
    ;; Block forever.
    (<!! (chan))))
