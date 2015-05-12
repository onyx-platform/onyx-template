(ns {{app-name}}.workflows.sample-workflow)

(def workflow
  [[:read-input :increment-age]
   [:increment-age :write-output]])
