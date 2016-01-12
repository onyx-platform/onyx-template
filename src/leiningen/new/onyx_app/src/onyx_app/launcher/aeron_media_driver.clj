(ns {{app-name}}.launcher.aeron-media-driver
  (:gen-class)
  (:require [clojure.core.async :refer [chan <!!]])
  (:import [uk.co.real_logic.aeron Aeron$Context]
           [uk.co.real_logic.aeron.driver MediaDriver MediaDriver$Context ThreadingMode]))

(defn -main [& [threading-mode]]
  (let [threading-mode (cond (or (nil? threading-mode)
                                 (= threading-mode "dedicated"))
                                 ThreadingMode/DEDICATED
                                 (= threading-mode "shared")
                                 ThreadingMode/SHARED
                                 (= threading-mode "shared-network")
                                 ThreadingMode/SHARED_NETWORK)
        _ (println "Starting media driver with threading mode: " threading-mode)
        ctx (-> (MediaDriver$Context.)
                (.threadingMode threading-mode))
        media-driver (MediaDriver/launch ctx)]
    (println "Launched the Media Driver. Blocking forever...")
    (<!! (chan))))