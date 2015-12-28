(ns {{app-name}}.jobs.sample-submit-job
    (:require [onyx.test-helper :refer [load-config]]
              [{{app-name}}.sample-input :refer [lines]]
              [{{app-name}}.catalogs.sample-catalog :refer [build-catalog]]
              [{{app-name}}.lifecycles.sample-lifecycle :refer [add-core-async
                                                                add-kafka
                                                                add-logging
                                                                add-sql
                                                                add-seq
                                                                add-metrics
                                                                build-lifecycles]]
              [{{app-name}}.workflows.sample-workflow :refer [build-workflow]]))

;;;; Lets build a job
(defn build-job [mode]
  (let [core-async?  (= :dev mode)
        kafka        (= :prod mode)
        seq          (if (= :dev mode) lines)
        sql          (= :prod mode)
        logging      :write-lines
        metrics      :write-lines
        base-job {:catalog (build-catalog {:batch-size    1
                                           :batch-timeout 1000
                                           :mode          mode})
                  :lifecycles (build-lifecycles {:mode mode})
                  :workflow (build-workflow {:mode mode})
                  :task-scheduler :onyx.task-scheduler/balanced}]
    (cond-> base-job
      core-async? (add-core-async)
      seq     (add-seq seq)
      metrics (add-metrics metrics)
      logging (add-logging logging)
      kafka   (add-kafka {:kafka/topic     "meetup"
                          :kafka/group-id  "onyx-consumer"
                          :kafka/zookeeper "zk:2181"
                          :kafka/partition "0"})
      sql (add-sql))))

(defn -main [onyx-id & args]
  (let [config (load-config "config.edn")
        peer-config (-> (get config :peer-config)
                        (assoc :onyx/id onyx-id)
                        (assoc :zookeeper/address "192.168.99.100:2181"))
        job (build-job :prod)]
    (onyx.api/submit-job peer-config job)))
