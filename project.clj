(defproject onyx-app/lein-template "0.9.0.9-SNAPSHOT"
  :description "Onyx Leiningen application template"
  :url "https://github.com/onyx-platform/onyx-template"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"snapshots" {:url "https://clojars.org/repo"
                              :username :env
                              :password :env
                              :sign-releases false}
                 "releases" {:url "https://clojars.org/repo"
                             :username :env
                             :password :env
                             :sign-releases false}}
  :plugins [[lein-set-version "0.4.1"]]
  :profiles {:dev {:plugins [[lein-set-version "0.4.1"]
                             [lein-update-dependency "0.1.2"]
                             [lein-pprint "1.1.1"]]}}
  :eval-in-leiningen true)
