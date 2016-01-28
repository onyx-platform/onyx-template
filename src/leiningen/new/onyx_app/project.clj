(defproject {{app-name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.onyxplatform/onyx "{{onyx-version}}"]
                 [org.onyxplatform/onyx-sql "{{onyx-version}}.{{onyx-sql-minor}}"]
                 [mysql/mysql-connector-java "5.1.18"]
                 [org.onyxplatform/onyx-kafka "{{onyx-version}}.{{onyx-kafka-minor}}"]
                 {{#metrics?}}[org.onyxplatform/onyx-metrics "{{onyx-version}}.{{onyx-metrics-minor}}"]{{/metrics?}}
                 [org.onyxplatform/onyx-seq "{{onyx-version}}.{{onyx-seq-minor}}"]
                 [cheshire "5.5.0"]
                 [aero "0.1.3"]]
  :profiles {:uberjar {:aot [{{app-name}}.launcher.aeron-media-driver
                             {{app-name}}.launcher.launch-prod-peers]}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
                   :source-paths ["src"]}})
