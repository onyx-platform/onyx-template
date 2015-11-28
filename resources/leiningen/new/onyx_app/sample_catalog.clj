(ns {{app-name}}.catalogs.sample-catalog)

;;; Catalogs describe each task in a workflow. We use
;;; them for describing input and output sources, injecting parameters,
;;; and adjusting performance settings.

(defn base-catalog [batch-size batch-timeout]
  [{:onyx/name :format-line
    :onyx/fn :testapp.functions.sample-functions/format-line
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
    :onyx/fn :testapp.functions.sample-functions/upper-case
    :onyx/type :function
    :onyx/batch-size batch-size
    :onyx/batch-timeout batch-timeout
    :onyx/doc "Capitalizes the first letter of the line"}])

(defmulti build-catalog :mode)

(defmethod build-catalog :dev
  [{:keys [batch-size batch-timeout]}]
  (into
    (base-catalog batch-size batch-timeout)
    [{:onyx/name :read-lines
      :onyx/plugin :onyx.plugin.core-async/input
      :onyx/type :input
      :onyx/medium :core.async
      :onyx/max-peers 1
      :onyx/batch-size batch-size
      :onyx/batch-timeout batch-timeout
      :onyx/doc "Reads segments from a core.async channel"}

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
      :onyx/plugin :testapp.plugins.http-reader/reader
      :onyx/type :input
      :onyx/medium :http
      :http/uri "http://textfiles.com/stories/abbey.txt"
      :onyx/batch-size batch-size
      :onyx/batch-timeout batch-timeout
      :onyx/max-peers 1
      :onyx/doc "Reads lines from an HTTP url text file"}

     {:onyx/name :write-lines
      :onyx/plugin :onyx.plugin.core-async/output
      :onyx/type :output
      :onyx/medium :core.async
      :onyx/batch-size batch-size
      :onyx/batch-timeout batch-timeout
      :onyx/max-peers 1
      :onyx/doc "Writes segments to a core.async channel"}]))
