(defproject {{app-name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-beta2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.mdrogalis/onyx "0.6.0-SNAPSHOT"]
                 [org.slf4j/slf4j-simple "1.6.1"]
                 [com.stuartsierra/component "0.2.3"]
                 [fipp "0.6.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]]
                   :source-paths ["env/dev" "src"]}})
