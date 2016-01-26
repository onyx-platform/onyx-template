(ns {{app-name}}.launcher.launch-prod-peers
  (:require [clojure.core.async :refer [<!! chan]]
            [environ.core :refer [env]]
            [onyx.test-helper :refer [load-config]]
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
{{#docker?}}
(defn standard-out-logger
  "Logger to output on std-out, for use with docker-compose"
  [data]
  (let [{:keys [output-fn]} data]
    (println (output-fn data))))
{{/docker?}}
(defn -main [onyx-id n & args]
  (let [n-peers (Integer/parseInt n)
        config (update-in (load-config "config.edn")
                          [:peer-config :zookeeper/address]
                          (fn [zkaddr]
                            (if zkaddr zkaddr "zk:2181"))) ;; Default to zk:2181 if none is specified
        peer-config (-> (:peer-config config)
                        (assoc :onyx/id onyx-id)
                        {{#docker?}}(assoc :onyx.log/config {:appenders
                                                             {:standard-out
                                                              {:enabled? true
                                                               :async? false
                                                               :output-fn t/default-output-fn
                                                               :fn standard-out-logger}}}){{/docker?}})
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
