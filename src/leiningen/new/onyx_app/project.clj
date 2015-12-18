(defproject {{app-name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.onyxplatform/onyx "{{onyx-version}}"]
                 [environ "1.0.1"]
                 ;Docker Opts
                 {{#docker?}}[org.onyxplatform/onyx-sql "0.8.2.1"]{{/docker?}}
                 {{#docker?}}[org.onyxplatform/onyx-kafka "0.8.2.2"]{{/docker?}}
                  [org.onyxplatform/onyx-seq "0.8.3.0"]
                  [cheshire "5.5.0"]
                  [mysql/mysql-connector-java "5.1.18"]
                 ]
  :profiles {:uberjar {:aot [{{app-name}}.launcher.aeron-media-driver
                             {{app-name}}.launcher.launch-prod-peers]}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
                   :source-paths ["src"]}})
