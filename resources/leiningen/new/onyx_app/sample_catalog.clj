(ns {{app-name}}.catalogs.sample-catalog)

(def batch-size 20)

(def catalog
  [{:onyx/name :read-input
    :onyx/ident :core.async/read-from-chan
    :onyx/type :input
    :onyx/medium :core.async
    :onyx/batch-size batch-size
    :onyx/max-peers 1
    :onyx/doc "Reads segments from a core.async channel"}

   {:onyx/name :increment-age
    :onyx/fn :{{app-name}}.functions.sample-functions/increment-age
    :onyx/type :function
    :onyx/batch-size batch-size
    :onyx/doc "Increments a user's age by 1"}

   {:onyx/name :write-output
    :onyx/ident :core.async/write-to-chan
    :onyx/type :output
    :onyx/medium :core.async
    :onyx/batch-size batch-size
    :onyx/max-peers 1
    :onyx/doc "Writes segments to a core.async channel"}])
