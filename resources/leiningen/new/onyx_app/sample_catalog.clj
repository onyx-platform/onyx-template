(ns {{app-name}}.catalogs.sample-catalog)

(defn in-memory-catalog
  "Takes a catalog and a set of input/output task names,
   returning a new catalog with all I/O catalog entries
   that were specified turned into core.async plugins. The
   core.async entries preserve all non-Onyx parameters."
  [catalog tasks]
  (mapv
   (fn [entry]
     (cond (and (some #{(:onyx/name entry)} tasks) (= (:onyx/type entry) :input))
           (merge
            entry
            {:onyx/ident :core.async/read-from-chan
             :onyx/type :input
             :onyx/medium :core.async
             :onyx/max-peers 1
             :onyx/doc "Reads segments from a core.async channel"})
           (and (some #{(:onyx/name entry)} tasks) (= (:onyx/type entry) :output))
           (merge
            entry
            {:onyx/ident :core.async/write-to-chan
             :onyx/type :output
             :onyx/medium :core.async
             :onyx/max-peers 1
             :onyx/doc "Writes segments to a core.async channel"})
           :else entry))
   catalog))

(defn build-catalog [batch-size]
  [{:onyx/name :read-lines
    :onyx/ident :http/read-lines
    :onyx/type :input
    :onyx/medium :http
    :http/uri "http://textfiles.com/stories/abbey.txt"
    :onyx/batch-size batch-size
    :onyx/max-peers 1
    :onyx/doc "Reads lines from an HTTP url text file"}

   {:onyx/name :format-line
    :onyx/fn :{{app-name}}.functions.sample-functions/format-line
    :onyx/type :function
    :onyx/batch-size batch-size
    :onyx/doc "Strips the line of any leading or trailing whitespace"}

   {:onyx/name :upper-case
    :onyx/fn :{{app-name}}.functions.sample-functions/upper-case
    :onyx/type :function
    :onyx/batch-size batch-size
    :onyx/doc "Capitalizes the first letter of the line"}

   {:onyx/name :write-lines
    :onyx/ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/batch-size batch-size
    :onyx/max-peers 1
    :onyx/doc "Writes segments to a core.async channel"}])
