(ns {{app-name}}.jobs.sample-submit-job
    (:require [{{app-name}}.catalogs.sample-catalog :refer [build-catalog]]
              [{{app-name}}.tasks.kafka :refer [add-kafka-input add-kafka-output]]
              [{{app-name}}.tasks.core-async :refer [add-core-async-input add-core-async-output]]
              [{{app-name}}.tasks.sql :refer [add-sql-input add-sql-output]]
              [{{app-name}}.tasks.file-input :refer [add-seq-file-input]]
              [{{app-name}}.lifecycles.sample-lifecycle :refer [add-logging add-metrics build-lifecycles]]
              [{{app-name}}.workflows.sample-workflow :refer [build-workflow]]
              [onyx.test-helper :refer [load-config]]))

;;;;
;; Lets build a job
;; Depending on the mode, the job is built up in a different way
;; When :dev mode, onyx-seq will be used as an input, with the meetup data being
;; included in the onyx-seq lifecycle for easy access
;; core.async is then added as an output task
;;
;; When using :prod mode, kafka is added as an input, and onyx-sql is used as the output

(defn build-job [mode]
  (let [batch-size 1
        batch-timeout 1000
        base-job {:catalog (build-catalog batch-size batch-timeout)
                  :lifecycles (build-lifecycles {:mode mode})
                  :workflow (build-workflow {:mode mode})
                  :task-scheduler :onyx.task-scheduler/balanced}]
    (cond-> base-job
      (= :dev mode) (add-core-async-output :write-lines batch-size)
      (= :dev mode) (add-seq-file-input :read-lines batch-size "resources/sample_input.edn")
      (= :prod mode) (add-kafka-input :read-lines "meetup" "onyx-consumer" "zk:2181" :json true {:kafka/offset-reset :smallest})
      (= :prod mode) (add-sql-output :write-lines {:sql/classname "com.mysql.jdbc.Driver"
                                                   :sql/subprotocol "mysql"
                                                   :sql/subname "//db:3306/meetup"
                                                   :sql/user "onyx"
                                                   :sql/password "onyx"
                                                   :sql/table :recentMeetups})
      {{#metrics?}}true (add-metrics :write-lines {:metrics/buffer-capacity 10000
                                                   :metrics/workflow-name "meetup-workflow"
                                                   :metrics/sender-fn :onyx.lifecycle.metrics.timbre/timbre-sender}){{/metrics?}}
      true (add-logging :write-lines))))

(defn -main [onyx-id & args]
  (let [config (load-config "config.edn")
        peer-config (-> (get config :peer-config)
                        (assoc :onyx/id onyx-id)
                        (assoc :zookeeper/address "192.168.99.100:2181"))
        job (build-job :prod)]
    (let [{:keys [job-id]} (onyx.api/submit-job peer-config job)]
      (println "Submitted job: " job-id))))
