(ns leiningen.new.onyx-app
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
            [clojure.java.shell :refer [sh]]
            [leiningen.core.main :as main]))

(def render (renderer "onyx_app"))

(defn docker? [opts]
  (some #{"+docker"} opts))

(defn files-to-render [opts]
  (cond-> ["README.md"
           ".gitignore"
           "LICENSE"
           "project.clj"
           "resources/config.edn"
           "src/onyx_app/core.clj"
           "src/onyx_app/jobs/basic.clj"
           "src/onyx_app/tasks/math.clj"
           "test/onyx_app/jobs/basic_test.clj"]
    (docker? opts) (conj "Dockerfile"
                         "docker-compose.yaml"
                         "scripts/finish_media_driver.sh"
                         "scripts/run_media_driver.sh"
                         "scripts/run_peer.sh")))

(defn render-files [files name data]
  (let [name (clojure.string/replace name #"-" "_")]
    (mapv (juxt (fn [path] (clojure.string/replace path #"onyx_app" name))
                (fn [file-path] (render file-path data)))
          files)))

(defn onyx-app
  "Creates a new Onyx application template"
  [name & args]
  (let [path (name-to-path name)
        data {:name name
              ;; The formatting here matters
              :onyx-version "0.9"
              :onyx-version-post ".7-SNAPSHOT"
              :lib-onyx-minor "0.1"
              :app-name name
              :app-name-underscore (clojure.string/replace name #"-" "_")
              :sanitized path
              :docker? (fn [block] (if (docker? args) block ""))}

        files (files-to-render args)
        render-instructions (render-files files name data)]
    (main/info "Generating fresh Onyx app.")
    (apply ->files data render-instructions)
    (run! (fn [file]
            (sh "chmod" "+x" (str name file)))
          ["/script/build.sh"])
    (when (docker? args)
      (run! (fn [file]
              (sh "chmod" "+x" (str name file)))
            ["/script/run_peers.sh" "/script/run_aeron.sh" "/script/kafka-meetup-streamer/script.sh"]))
    (main/info (str "Building a new onyx app with: " args))))
