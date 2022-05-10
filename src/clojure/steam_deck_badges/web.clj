(ns steam-deck-badges.web
  (:require [hickory.core :as hickory]
            [org.httpkit.client :as http]
            [org.httpkit.sni-client :as sni-client])
  (:import (java.util Map)))

(alter-var-root #'org.httpkit.client/*default-client* (fn [_] sni-client/default-client))

(def tag
  "returns the tag keyword of the hiccup element"
  first)

(def attribs
  "returns the attribute map of the hiccup element"
  second)

(def body
  "returns the children/body of the hiccup element"
  nnext)


(defn ^:private submap?
  "Checks whether m contains all entries in sub."
  [^Map sub ^Map m case-sensitive?]
  (loop [ks (keys sub)]
    (or (nil? ks)
        (let [a (get sub (first ks) ::NOT_FOUND)
              b (get m (first ks) ::NOT_FOUND)]
          (if (or (= a b)
                  (and (not case-sensitive?)
                       (string? a)
                       (string? b)
                       (= (.toLowerCase ^String a) (.toLowerCase ^String b))))
            (recur (next ks))
            false)))))


(defn search-all
  "Searches a hiccup element hierarchy for elements with the given tag and matching attributes"
  [element target-tag target-attribs & [case-sensitive?]]
  (when (vector? element)
    (let [match? (and (or (nil? target-tag) (= (tag element) target-tag))
                      (submap? target-attribs (attribs element) case-sensitive?))]
      (loop [children (body element)
             results (if match? [element] [])]
        (if (empty? children)
          results
          (recur (rest children)
                 (into results (search-all (first children) target-tag target-attribs case-sensitive?))))))))


(defn search
  "Searches a hiccup element hierarchy for the first element with the given tag and matching attributes"
  [element target-tag target-attribs & [case-sensitive?]]
  (first (search-all element target-tag target-attribs case-sensitive?)))


(defn parse-html
  "Parses the given HTML string to a hiccup data structure."
  [html]
  (->> html
       hickory/parse
       hickory/as-hiccup
       (filter vector?)
       first))


(defn load-html [url-or-request]
  (some->
    (http/request (if (string? url-or-request)
                    {:method :get :url url-or-request}
                    url-or-request))
    deref
    :body))


(defn load-hiccup [url-or-request]
  (some->
    url-or-request
    load-html
    parse-html))
