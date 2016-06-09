(defproject {{app-name}} "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {:name ""
            :url ""}
  :dependencies [[aero "1.0.0-beta2"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.onyxplatform/onyx "{{onyx-version}}{{onyx-version-post}}"]
                 [org.onyxplatform/lib-onyx "{{onyx-version}}.{{lib-onyx-minor}}"]]
  :source-paths ["src"]
  :profiles {:dev {:jvm-opts ["-XX:-OmitStackTraceInFastThrow"]}
             :dependencies [[org.clojure/tools.namespace "0.2.11"]
                            [lein-project-version "0.1.0"]]
             :uberjar {:aot [lib-onyx.media-driver
                             {{app-name}}.core]
                       :uberjar-name "peer.jar"}})
