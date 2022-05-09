(ns steam-deck-badges.core
  (:require [steam-deck-badges.badge :as badge])
  (:gen-class))

;; TODO: implement small server which delivers the badges

(defn ^:private compatibility-badge-for-app
  [app-id size]
  (-> (parse-long app-id)
      (badge/badge-for-app size)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
