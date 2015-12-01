(ns leiningen.new.onyx-app
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
            [leiningen.core.main :as main]))

(def render (renderer "onyx_app"))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))

(defn onyx-app
  "Creates a new Onyx application template"
  [name & args]
  (let [path (name-to-path name)
        onyx-version "0.8.2"
        data {:name name
              :onyx-version onyx-version
              :app-name name
              :sanitized path}
        base-files [["README.md" (render "README.md" data)]
                    [".gitignore" (render "gitignore" data)]
                    ["LICENSE" (render "LICENSE" data)]
                    ["project.clj" (render "project.clj" data)]
                    ["Dockerfile" (render "Dockerfile" data)]
                    [(str "script/run-container.sh") (render "run-container.sh" data)]
                    [(str "script/run-peers.sh") (render "run-peers.sh" data)]
                    ["script/build.sh" (render "build.sh" data)]
                    ["env/dev/user.clj" (render "user.clj" data)]
                    [(str "env/dev/" path "/dev_inputs/sample_input.clj") (render "sample_input.clj" data)]
                    ["resources/config.edn" (render "config.edn" data)]
                    [(str "src/" path "/utils.clj") (render "utils.clj" data)]
                    [(str "src/" path "/launcher/aeron_media_driver.clj") (render "aeron_media_driver.clj" data)]]
        sample-files [[(str "src/" path "/launcher/launch_prod_peers.clj") (render "launch_prod_peers.clj" data)]
                      [(str "src/" path "/workflows/sample_workflow.clj") (render "sample_workflow.clj" data)]
                      [(str "src/" path "/catalogs/sample_catalog.clj") (render "sample_catalog.clj" data)]
                      [(str "src/" path "/flow_conditions/sample_flow_conditions.clj") (render "sample_flow_conditions.clj" data)]
                      [(str "src/" path "/functions/sample_functions.clj") (render "sample_functions.clj" data)]
                      [(str "src/" path "/lifecycles/sample_lifecycle.clj") (render "sample_lifecycle.clj" data)]
                      [(str "src/" path "/plugins/http_reader.clj") (render "http_reader.clj" data)]
                      [(str "src/" path "/jobs/sample_submit_job.clj") (render "sample_submit_job.clj" data)]
                      [(str "test/" path "/jobs/sample_job_test.clj") (render "sample_job_test.clj" data)]]
        bare-files [[(str "src/" path "/launcher/launch_prod_peers.clj") (render "launch_prod_peers_bare.clj" data)]]]
    (main/info "Generating fresh Onyx app.")
    (main/info (str args))
    (apply ->files 
           data
           (cond-> base-files
             (nil? args) (into sample-files)
             (= args "bare") (into bare-files)))))
["Dockerfile" "script/run-container.sh" "script/run-peers.sh" "script/build.sh"]
(defn files-to-render [opts]
  (cond -> ["README.md" ".gitignore"
            "LICENSE"   "project.clj"
            "env/dev/user.clj"
            "env/dev/onyx-app/sample_input.clj"
            "resources/config.edn"
            ""]))

(defn docker? [opts]
  (some #{"+docker"} opts))

(defn metrics? [opts]
  (some #{"+metrics"} opts))

(defn metrics-requires [opts]
  (if (metrics? opts)
    ["onyx.lifecycle.metrics.metrics"]
    ["onyx.lifecycle.metrics.timbre"]))

(defn option-mapping [name opts]
  {:docker? (fn [expr] (if (:docker? opts) expr ""))})