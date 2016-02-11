(ns leiningen.new.onyx-app
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
	    [clojure.java.shell :refer [sh]]
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
           "resources/sample_input.edn"
           "src/onyx_app/launcher/launch_prod_peers.clj"
           "src/onyx_app/launcher/aeron_media_driver.clj"
           "src/onyx_app/jobs/sample_submit_job.clj"
           "src/onyx_app/tasks/kafka.clj"
           "src/onyx_app/tasks/file_input.clj"
           "src/onyx_app/tasks/sql.clj"
           "src/onyx_app/tasks/core_async.clj"
           "src/onyx_app/tasks/meetup_tasks.clj"
           "src/onyx_app/behaviors/logging.clj"
           "src/onyx_app/utils/job.clj"
           "test/onyx_app/jobs/sample_job_test.clj"
           "script/build.sh"]
    (metrics? opts) (conj "src/onyx_app/behaviors/metrics.clj")
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
	data {:name name
	      ;; The formatting here matters
	      :onyx-version "0.8.9"
	      :onyx-sql-minor "0"
	      :onyx-kafka-minor "0"
	      :onyx-metrics-minor "0"
	      :onyx-seq-minor "0"
	      :app-name name
	      :sanitized path
	      :docker? (fn [block] (if (docker? args) block ""))
	      :metrics? (fn [block] (if (metrics? args) block ""))}

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
