(ns {{app-name}}.tasks.math)

(defn inc-in-segment
  "A specialized version of update-in that increments a key in a segment"
  [ks segment]
  (update-in segment ks inc))

(defn inc-key [task-name ks task-opts]
  {:task {:task-map (merge {:onyx/name task-name
                            :onyx/type :function
                            :onyx/fn ::inc-in-segment
                            ::inc-key ks
                            :onyx/params [::inc-key]}
                           task-opts)}})
