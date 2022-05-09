(ns steam-deck-badges.steam
  (:require [steam-deck-badges.web :as web]
            [clojure.data.json :as json])
  (:import (com.github.benmanes.caffeine.cache Cache Caffeine)
           (java.util.concurrent TimeUnit)))

(def ^:private ^Cache cache
  (-> (Caffeine/newBuilder)
      (.expireAfterWrite 1 TimeUnit/DAYS)
      (.build)))

(defn ^:private find-deck-compatibility-level
  "Scrapes Steam to determine the Steam-Deck compatibility level for the given app-id."
  [app-id]
  (let [page        (web/load-hiccup {:method  :get
                                      :url     (str "https://store.steampowered.com/app/" app-id)
                                      :headers {"Cookie" "wants_mature_content=1; birthtime=0; lastagecheckage=1-1-1970"}})
        compat-data (some-> (web/search page :div {:id "application_config"})
                            second
                            :data-deckcompatibility
                            (json/read-str))
        category    (get compat-data "resolved_category" -1)]
    (case (int category)
      1 :unsupported
      2 :playable
      3 :verified
      :unknown)))

(defn deck-compatibility-level
  "Returns the Steam Deck compatibility level for a given Steam app-id.
  (:verified, :playable, :unsupported, or :unknown)"
  [^long app-id]
  (if-not (> app-id 0)
    :unknown
    (let [app-id (long app-id)]
      (if-let [cached (.getIfPresent cache app-id)]
        cached
        (let [level (find-deck-compatibility-level app-id)]
          (.put cache app-id level)
          level)))))
