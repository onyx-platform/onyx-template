(ns {{app-name}}.catalogs.sample-catalog)

;;; Catalogs describe each task in a workflow. We use
;;; them for describing input and output sources, injecting parameters,
;;; and adjusting performance settings.

(defn build-catalog
  ([] (build-catalog 5 50))
  ([batch-size batch-timeout]
     [{:onyx/name :read-lines
       :onyx/plugin :{{app-name}}.plugins.http-reader/reader
       :onyx/type :input
       :onyx/medium :http
       :http/uri "http://textfiles.com/stories/abbey.txt"
       :onyx/batch-size batch-size
       :onyx/batch-timeout batch-timeout
       :onyx/max-peers 1
       :onyx/doc "Reads lines from an HTTP url text file"}

      {:onyx/name :format-line
       :onyx/fn :{{app-name}}.functions.sample-functions/format-line
       :onyx/type :function
       :onyx/batch-size batch-size
       :onyx/batch-timeout batch-timeout
       :onyx/doc "Strips the line of any leading or trailing whitespace"}

      {:onyx/name :upper-case
       :onyx/fn :{{app-name}}.functions.sample-functions/upper-case
       :onyx/type :function
       :onyx/batch-size batch-size
       :onyx/batch-timeout batch-timeout
       :onyx/doc "Capitalizes the first letter of the line"}

      {:onyx/name :write-lines
       :onyx/plugin :onyx.plugin.core-async/output
       :onyx/type :output
       :onyx/medium :core.async
       :onyx/batch-size batch-size
       :onyx/batch-timeout batch-timeout
       :onyx/max-peers 1
       :onyx/doc "Writes segments to a core.async channel"}]))
