(ns {{app-name}}.launcher.launch-prod-peers
  (:require [clojure.core.async :refer [<!! chan]]
            [environ.core :refer [env]]
            [onyx.test-helper :refer [load-config]]
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

(defn -main [onyx-id n & args]
  (let [n-peers (Integer/parseInt n)
        config (update-in (load-config "config.edn")
                          [:peer-config :zookeeper/address]
                          (fn [zkaddr]
                            (if zkaddr zkaddr "zk:2181"))) ;; Default to zk:2181 if none is specified
        peer-config (assoc (:peer-config config) :onyx/id onyx-id)
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
