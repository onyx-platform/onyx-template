(ns {{app-name}}.functions.sample-functions)

(defn increment-age [segment]
  (update-in segment [:age] inc))
