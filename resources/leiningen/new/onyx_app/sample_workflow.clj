(ns {{app-name}}.workflows.sample-workflow)

(def workflow
  [[:read-lines :format-line]
   [:format-line :upper-case]
   [:upper-case :write-lines]])
