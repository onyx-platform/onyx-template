(ns {{app-name}}.tasks.kafka
    (:require [taoensso.timbre :refer [info]]
              [cheshire.core :as json]))

(defn deserialize-message-json [bytes]
  (try
    (json/parse-string (String. bytes "UTF-8"))
    (catch Exception e
      {:error e})))

(defn deserialize-message-edn [bytes]
  (try
    (read-string (String. bytes "UTF-8"))
    (catch Exception e
      {:error e})))

(defn serialize-message-json [segment]
  (.getBytes (json/generate-string segment)))

(defn serialize-message-edn [segment]
  (.getBytes segment))

(defn expand-serializer-fn [v]
  (condp = v
    :json    ::serialize-message-json
    :edn     ::serialize-message-edn
    v))

(defn expand-deserializer-fn [v]
  (condp = v
    :json    ::deserialize-message-json
    :edn     ::deserialize-message-edn
    v))

(defn add-kafka-input
  "Instrument a job with Kafka lifecycles and catalog entries."
  ([job task topic group-id zookeeper-addr batch-size deserializer-fn force-reset?] 
   (add-kafka-input job task topic group-id zookeeper-addr batch-size deserializer-fn force-reset? {}))
  ([job task topic group-id zookeeper-addr batch-size deserializer-fn force-reset? opts]
   (-> job
       (update :catalog conj {:onyx/name task
                              :onyx/plugin :onyx.plugin.kafka/read-messages
                              :onyx/type :input
                              :onyx/medium :kafka
                              :kafka/topic topic
                              :kafka/group-id group-id
                              :kafka/fetch-size 307200
                              :kafka/chan-capacity 1000
                              :kafka/zookeeper zookeeper-addr
                              :kafka/offset-reset :smallest
                              :kafka/force-reset? force-reset?
                              :kafka/empty-read-back-off 500
                              :kafka/commit-interval 500
                              :kafka/deserializer-fn (expand-deserializer-fn deserializer-fn)
                              :onyx/batch-size batch-size
                              :onyx/doc "Reads messages from a Kafka topic"})
       (update :lifecycles conj {:lifecycle/task task
                                 :lifecycle/calls :onyx.plugin.kafka/read-messages-calls}))))

(defn add-kafka-output
  "Instrument a job with Kafka lifecycles and catalog entries."
  ([job task topic zookeeper-addr batch-size serializer-fn]
   (add-kafka-output job task topic zookeeper-addr batch-size serializer-fn {}))
  ([job task topic zookeeper-addr batch-size serializer-fn opts]
   (-> job
       (update :catalog conj (merge {:onyx/name task
                                     :onyx/plugin :onyx.plugin.kafka/write-messages
                                     :onyx/type :output
                                     :onyx/medium :kafka
                                     :kafka/topic topic
                                     :kafka/zookeeper zookeeper-addr
                                     :kafka/serializer-fn (expand-serializer-fn serializer-fn)
                                     :kafka/request-size 307200
                                     :onyx/batch-size batch-size
                                     :onyx/doc "Writes messages to a Kafka topic"}
                                    opts))
       (update :lifecycles conj {:lifecycle/task task
                                 :lifecycle/calls :onyx.plugin.kafka/write-messages-calls}))))
