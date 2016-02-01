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

{{#docker?}}
(defn standard-out-logger
  "Logger to output on std-out, for use with docker-compose"
  [data]
  (let [{:keys [output-fn]} data]
    (println (output-fn data))))
{{/docker?}}

(defn -main [n & args]
  (let [n-peers (Integer/parseInt n)
        config (read-config (clojure.java.io/resource "config.edn") {:profile :default})
        peer-config (-> (:peer-config config)
                        {{#docker?}}(assoc :onyx.log/config {:appenders
                                                             {:standard-out
                                                              {:enabled? true
                                                               :async? false
                                                               :output-fn t/default-output-fn
                                                               :fn standard-out-logger}}}){{/docker?}})
        peer-group (onyx.api/start-peer-group peer-config)
        env (onyx.api/start-env (:env-config config))
        peers (onyx.api/start-peers n-peers peer-group)]
    (println "Attempting to connect to to Zookeeper: " (:zookeeper/address peer-config))
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
