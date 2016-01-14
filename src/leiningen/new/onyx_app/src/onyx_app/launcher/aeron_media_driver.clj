(ns {{app-name}}.launcher.aeron-media-driver
  (:gen-class)
  (:require [clojure.core.async :refer [chan <!!]])
  (:import [uk.co.real_logic.aeron Aeron$Context]
           [uk.co.real_logic.aeron.driver MediaDriver MediaDriver$Context ThreadingMode]))

(defn -main [& [threading-mode]]
  (when (and (System/getProperty "aeron.threading.mode")
             threading-mode)
    (throw (Exception. "Cannot set both aeron.threading.mode property and threading-mode command-line arg")))

  (let [threading-mode (cond (or (nil? threading-mode)
                                 (= threading-mode "shared"))
                             ThreadingMode/SHARED
                             (= threading-mode "dedicated")
                             ThreadingMode/DEDICATED
                             (= threading-mode "shared-network")
                             ThreadingMode/SHARED_NETWORK)
        _ (println "Starting media driver with threading mode:" threading-mode)
        ctx (cond-> (MediaDriver$Context.)
              threading-mode (.threadingMode threading-mode))
        media-driver (MediaDriver/launch ctx)]
    (println "Launched the Media Driver. Blocking forever...")
    (<!! (chan))))
