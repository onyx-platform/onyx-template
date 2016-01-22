(ns {{app-name}}.jobs.sample-submit-job
    (:require [{{app-name}}.catalogs.sample-catalog :refer [build-catalog]]
              [{{app-name}}.lifecycles.sample-lifecycle
               :refer [add-core-async-output add-kafka-input add-logging
                       add-sql-output add-seq add-metrics build-lifecycles]]
              [{{app-name}}.sample-input :refer [lines]]
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
  (let [core-async? (= :dev mode)
        kafka? (= :prod mode)
        seq (if (= :dev mode) lines)
        sql? (= :prod mode)
        logging :write-lines
        {{#metrics?}}metrics :write-lines{{/metrics?}}
        base-job {:catalog (build-catalog {:batch-size 1
                                           :batch-timeout 1000
                                           :mode mode})
                  :lifecycles (build-lifecycles {:mode mode})
                  :workflow (build-workflow {:mode mode})
                  :task-scheduler :onyx.task-scheduler/balanced}]
    (cond-> base-job
      core-async? (add-core-async-output :write-lines)
      seq (add-seq seq)
      {{#metrics?}}metrics (add-metrics metrics){{/metrics?}}
      logging (add-logging logging)
      kafka? (add-kafka-input :write-lines {:kafka/topic "meetup"
                                            :kafka/group-id "onyx-consumer"
                                            :kafka/zookeeper "zk:2181"
                                            :kafka/partition "0"})
      sql? (add-sql-output :write-lines {:sql/classname "com.mysql.jdbc.Driver"
                                         :sql/subprotocol "mysql"
                                         :sql/subname "//db:3306/meetup"
                                         :sql/user "onyx"
                                         :sql/password "onyx"
                                         :sql/table :recentMeetups}))))

(defn -main [onyx-id & args]
  (let [config (load-config "config.edn")
        peer-config (-> (get config :peer-config)
                        (assoc :onyx/id onyx-id)
                        (assoc :zookeeper/address "192.168.99.100:2181"))
        job (build-job :prod)]
    (let [{:keys [job-id]} (onyx.api/submit-job peer-config job)]
      (println "Submitted job: " job-id))))
