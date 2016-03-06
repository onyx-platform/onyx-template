(ns {{app-name}}.launcher.launch-prod-peers
  (:gen-class)
  (:require [aero.core :refer [read-config]]
            [clojure.core.async :refer [<!! chan]]
            [{{app-name}}.jobs.meetup-job]
            [onyx.plugin.kafka]
            [onyx.plugin.sql]
            {{#docker?}}[taoensso.timbre :as t]{{/docker?}}
            {{#docker?}}[taoensso.timbre.appenders.3rd-party.rotor :as rotor]{{/docker?}}
            {{#metrics?}}[onyx.lifecycle.metrics.metrics]{{/metrics?}}
            {{#metrics?}}[onyx.lifecycle.metrics.timbre]{{/metrics?}}))

(defn standard-out-logger
  "Logger to output on std-out, for use with docker-compose"
  [data]
  (let [{:keys [output-fn]} data]
    (println (output-fn data))))

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
                                                             :rotor (-> (rotor/rotor-appender
                                                                          {:path "onyx.log"
                                                                           :max-size (* 512 102400)
                                                                           :backlog 5})
                                                                        (assoc :min-level :info))
                                                             {:standard-out
                                                              {:enabled? true
                                                               :async? false
                                                               :output-fn t/default-output-fn
                                                               :fn standard-out-logger}}}){{/docker?}})
        peer-group (onyx.api/start-peer-group peer-config)
        env (onyx.api/start-env (:env-config config))
        peers (onyx.api/start-peers n-peers peer-group)]
    (println "Attempting to connect to Zookeeper @" (:zookeeper/address peer-config))
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
