(ns {{app-name}}.tasks.kafka
    (:require [cheshire.core :as json]
              [schema.core :as s]
              [onyx.schema :as os]))

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

(s/defschema KafkaInputTask
  {(s/required-key :kafka/topic) s/Str
   (s/required-key :kafka/group-id) s/Str
   (s/required-key :kafka/zookeeper) s/Str
   (s/required-key :kafka/offset-reset) (s/enum :smallest :largest)
   (s/required-key :kafka/force-reset?) s/Bool
   (s/required-key :kafka/deserializer-fn) os/NamespacedKeyword
   (s/optional-key :kafka/chan-capacity) s/Num
   (s/optional-key :kafka/fetch-size) s/Num
   (s/optional-key :kafka/empty-read-back-off) s/Num
   (s/optional-key :kafka/commit-interval) s/Num})

(s/defschema KafkaOutputTask
  {(s/required-key :kafka/topic) s/Str
   (s/required-key :kafka/zookeeper) s/Str
   (s/required-key :kafak/serializer-fn) os/NamespacedKeyword
   (s/optional-key :kafka/request-size) s/Num})

(s/defn input-task
  [task-name :- s/Keyword opts :- KafkaInputTask]
  {:task {:task-map (merge {:onyx/name task-name
                            :onyx/plugin :onyx.plugin.kafka/read-messages
                            :onyx/type :input
                            :onyx/medium :kafka
                            :onyx/doc "Reads messages from a Kafka topic"}
                           opts)
          :lifecycles [{:lifecycle/task task-name
                        :lifecycle/calls :onyx.plugin.kafka/read-messages-calls}]}
   :schema {:task-map (merge os/TaskMap KafkaInputTask)
            :lifecycles [os/Lifecycle]}})

(s/defn output-task
  [task-name :- s/Keyword opts :- KafkaOutputTask]
  {:task {:task-map (merge {:onyx/name task-name
                            :onyx/plugin :onyx.plugin.kafka/write-messages
                            :onyx/type :output
                            :onyx/medium :kafka
                            :onyx/doc "Writes messages to a Kafka topic"}
                           opts)
          :lifecycles [{:lifecycle/task task-name
                        :lifecycle/calls :onyx.plugin.kafka/write-messages-calls}]}
   :schema {:task-map (merge os/TaskMap KafkaOutputTask)
            :lifecycles [os/Lifecycle]}})
