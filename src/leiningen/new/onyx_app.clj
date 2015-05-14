(ns leiningen.new.onyx-app
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
            [leiningen.core.main :as main]))

(def render (renderer "onyx_app"))

(defn onyx-app
  "Creates a new Onyx application template"
  [name]
  (let [path (name-to-path name)
        data {:name name
              :app-name name
              :sanitized path}]
    (main/info "Generating fresh 'lein new' onyx project.")
    (->files data
             ["README.md" (render "README.md" data)]
             [".gitignore" (render "gitignore" data)]
             ["LICENSE" (render "LICENSE" data)]
             ["project.clj" (render "project.clj" data)]
             ["Dockerfile" (render "Dockerfile" data)]

             ["script/run-app.sh" (render "run-app.sh" data)]
             ["script/build.sh" (render "build.sh" data)]

             ["env/dev/user.clj" (render "user.clj" data)]
             [(str "env/dev/" path "/dev_inputs/sample_input.clj") (render "sample_input.clj" data)]

             ["resources/env-config.edn" (render "env-config.edn" data)]
             ["resources/dev-peer-config.edn" (render "dev-peer-config.edn" data)]
             ["resources/prod-peer-config.edn" (render "prod-peer-config.edn" data)]

             [(str "src/" path "/launcher/launch_prod_peers.clj") (render "launch_prod_peers.clj" data)]
             [(str "src/" path "/launcher/submit_prod_sample_job.clj") (render "submit_prod_sample_job.clj" data)]

             [(str "src/" path "/workflows/sample_workflow.clj") (render "sample_workflow.clj" data)]
             [(str "src/" path "/catalogs/sample_catalog.clj") (render "sample_catalog.clj" data)]
             [(str "src/" path "/flow_conditions/sample_flow_conditions.clj") (render "sample_flow_conditions.clj" data)]
             [(str "src/" path "/functions/sample_functions.clj") (render "sample_functions.clj" data)]
             [(str "src/" path "/lifecycles/sample_lifecycle.clj") (render "sample_lifecycle.clj" data)]
             [(str "src/" path "/plugins/http_reader.clj") (render "http_reader.clj" data)]

             [(str "test/" path "/jobs/sample_job_test.clj") (render "sample_job_test.clj" data)])))
