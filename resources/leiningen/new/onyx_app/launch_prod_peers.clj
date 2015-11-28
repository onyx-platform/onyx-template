(ns {{app-name}}.launcher.launch-prod-peers
  (:gen-class)
  (:require [clojure.core.async :refer [chan <!!]]
            [clojure.java.io :refer [resource]]
            [{{app-name}}.lifecycles.sample-lifecycle]
            [{{app-name}}.functions.sample-functions]
            [{{app-name}}.plugins.http-reader]
            [onyx.plugin.core-async]
            [onyx.test-helper :refer [load-config]]
            [onyx.api]))

(defn -main [onyx-id n & args]
  (let [n-peers (Integer/parseInt n)
        config (assoc-in (load-config "config.edn") [:peer-config :onyx/id] onyx-id)
        peer-config (get config :peer-config)
	      peer-group (onyx.api/start-peer-group peer-config)
	      peers (onyx.api/start-peers n-peers peer-group)]
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
