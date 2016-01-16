(ns {{app-name}}.catalogs.sample-catalog
    (:require [{{app-name}}.functions.sample-functions]))

;;; Catalogs describe each task in a workflow. We use
;;; them for describing input and output sources, injecting parameters,
;;; and adjusting performance settings.

(defn base-catalog [batch-size batch-timeout]
  [{:onyx/name :format-line
    :onyx/fn :{{app-name}}.functions.sample-functions/format-line
    :onyx/type :function
    :onyx/batch-size batch-size
    :onyx/batch-timeout batch-timeout
    :onyx/doc "Strips the line of any leading or trailing whitespace"}

   {:onyx/name :identity
    :onyx/fn :clojure.core/identity
    :onyx/type :function
    :onyx/batch-size batch-size
    :onyx/batch-timeout batch-timeout
    :onyx/doc "identity"}

   {:onyx/name :upper-case
    :onyx/fn :{{app-name}}.functions.sample-functions/upper-case
    :onyx/type :function
    :onyx/batch-size batch-size
    :onyx/batch-timeout batch-timeout
    :onyx/doc "Capitalizes the first letter of the line"}

   {:onyx/name :extract-meetup-info
    :onyx/fn :{{app-name}}.functions.sample-functions/transform-segment-shape
    :onyx/type :function
    :onyx/batch-size batch-size
    :onyx/batch-timeout batch-timeout
    :keypath {"groupId" ["group" "id"]
              "groupCity" ["group" "city"]
              "category" ["group" "category" "name"]}
    :onyx/params [:keypath]
    :onyx/doc "Extracts group-id group-city and category"}

   {:onyx/name :prepare-rows
    :onyx/fn :{{app-name}}.functions.sample-functions/prepare-rows
    :onyx/type :function
    :onyx/batch-size batch-size
    :onyx/batch-timeout batch-timeout}])

(defmulti build-catalog :mode)

(defmethod build-catalog :dev
  [{:keys [batch-size batch-timeout]}]
  (into
    (base-catalog batch-size batch-timeout)
    [{:onyx/name :read-lines
      :onyx/plugin :onyx.plugin.seq/input
      :onyx/type :input
      :onyx/medium :seq
      :seq/checkpoint? true
      :onyx/batch-size batch-size
      :onyx/max-peers 1
      :onyx/doc "Reads segments from seq"}

     {:onyx/name :write-lines
      :onyx/plugin :onyx.plugin.core-async/output
      :onyx/type :output
      :onyx/medium :core.async
      :onyx/max-peers 1
      :onyx/batch-size batch-size
      :onyx/batch-timeout batch-timeout
      :onyx/doc "Writes segments to a core.async channel"}]))

(defmethod build-catalog :prod
  [{:keys [batch-size batch-timeout]}]
  (into
    (base-catalog batch-size batch-timeout)
    [{:onyx/name :read-lines
      :onyx/plugin :onyx.plugin.kafka/read-messages
      :onyx/type :input
      :onyx/medium :kafka
      :onyx/batch-size batch-size
      :onyx/max-peers 1
      :onyx/doc "Read messages from a kafka topic"}

     {:onyx/name   :write-lines
      :onyx/plugin :onyx.plugin.sql/write-rows
      :onyx/type   :output
      :onyx/medium :sql
      :sql/classname "com.mysql.jdbc.Driver"
      :sql/subprotocol "mysql"
      :sql/subname "//db:3306/meetup"
      :sql/user "onyx"
      :sql/password "onyx"
      :sql/table :recentMeetups
      :onyx/batch-size batch-size}]))
;; TODO: Write an add-sql plugin that injects this data.
