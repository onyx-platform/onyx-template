(ns leiningen.new.onyx-app
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
            [leiningen.core.main :as main]))

(def render (renderer "onyx_app"))

(defn docker? [opts]
  (some #{"+docker"} opts))

(defn metrics? [opts]
  (some #{"+metrics"} opts))

(defn files-to-render [opts]
  (cond-> ["README.md" ".gitignore"
           "LICENSE" "project.clj"
           ".projectile"
           "resources/config.edn"
           "src/onyx_app/launcher/launch_prod_peers.clj"
           "src/onyx_app/launcher/aeron_media_driver.clj"
           "src/onyx_app/workflows/sample_workflow.clj"
           "src/onyx_app/catalogs/sample_catalog.clj"
           "src/onyx_app/functions/sample_functions.clj"
           "src/onyx_app/lifecycles/sample_lifecycle.clj"
           "src/onyx_app/jobs/sample_submit_job.clj"
           "src/onyx_app/utils.clj"
           "src/onyx_app/sample_input.clj"
           "test/onyx_app/jobs/sample_job_test.clj"
           "script/build.sh"]
    (docker? opts) (conj "Dockerfile"
                         "script/run_peers.sh"
                         "script/run_aeron.sh"
                         "script/kafka-meetup-streamer/Dockerfile"
                         "script/kafka-meetup-streamer/script.sh"
                         "docker-compose.yml")))

(defn render-files [files name data]
  (let [name (clojure.string/replace name #"-" "_")]
    (mapv (juxt (fn [path] (clojure.string/replace path #"onyx_app" name))
                (fn [file-path] (render file-path data)))
          files)))

(defn onyx-app
  "Creates a new Onyx application template"
  [name & args]
  (let [path (name-to-path name)
        onyx-version "0.8.4"
        onyx-sql-minor "0"
        onyx-kafka-minor "0"
        onyx-seq-minor "0"
        data {:name name
              :onyx-version onyx-version
              :app-name name
              :sanitized path
              :docker? (fn [block] (if (docker? args) block ""))
              :metrics? (fn [block] (if (metrics? args) block ""))}

        files (files-to-render args)
        render-instructions (render-files files name data)]
    (main/info "Generating fresh Onyx app.")
    (apply ->files data render-instructions)
    (main/info (str "Building a new onyx app with: " args))))
