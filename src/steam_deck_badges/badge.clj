(ns steam-deck-badges.badge
  (:require [clojure.java.io :as io]
            [steam-deck-badges.web :as web]
            [clojure.data.json :as json])
  (:import (java.awt.geom AffineTransform)
           (java.awt.image AffineTransformOp BufferedImage)
           (java.io ByteArrayOutputStream)
           (java.util.concurrent ConcurrentHashMap TimeUnit)
           (java.util.function Function)
           (javax.imageio ImageIO)
           (com.github.benmanes.caffeine.cache Cache Caffeine)
           (java.net URL)))

(defn ^:private new-image
  ^BufferedImage [w h]
  (BufferedImage. w h BufferedImage/TYPE_INT_ARGB))

(defn ^:private load-image
  ^BufferedImage [^URL img-url]
  (ImageIO/read img-url))


(defmulti ^:private badge-img identity)

(defmethod badge-img :verified [_]
  (load-image (io/resource "images/badge_verified_54.png")))

(defmethod badge-img :playable [_]
  (load-image (io/resource "images/badge_playable_54.png")))

(defmethod badge-img :unsupported [_]
  (load-image (io/resource "images/badge_unsupported_54.png")))

(defmethod badge-img :unknown [_]
  (load-image (io/resource "images/badge_unknown_54.png")))


(defn ^:private write-to-file
  [^BufferedImage image file]
  (ImageIO/write image "png" (io/file file)))


(defn ^:private write-to-bytes
  ^bytes [^BufferedImage image]
  (let [baos (ByteArrayOutputStream. 100000)]
    (ImageIO/write image "png" baos)
    (.toByteArray baos)))


(defn ^:private scale-image
  ^BufferedImage [^BufferedImage img ^double scale]
  (if (= scale 1.0)
    (let [w (.getWidth img)
          h (.getHeight img)]
      (.getSubimage img 0 0 w h))
    (let [w               (.getWidth img)
          h               (.getHeight img)
          new-img         (new-image (int (* w scale)) (int (* h scale)))
          g               (.createGraphics new-img)
          scale-transform (AffineTransform/getScaleInstance scale scale)
          interpolation   (if (< scale 1.0)
                            AffineTransformOp/TYPE_BILINEAR
                            AffineTransformOp/TYPE_BICUBIC)
          op              (AffineTransformOp. scale-transform interpolation)]
      (.drawImage g img op 0 0)
      new-img)))


(defn ^:private resize-image
  ^BufferedImage [^BufferedImage img ^long new-height]
  (scale-image img (/ (double new-height) (.getHeight img))))


(def ^:private ^ConcurrentHashMap badge-cache (ConcurrentHashMap.))

(defn ^:private badge-bytes
  "Returns PNG image bytes for a given badge compatibility type and size."
  [type size]
  (when (and (#{:verified :playable :unsupported :unknown} type)
             (<= 20 size 54))
    (.computeIfAbsent badge-cache
      [type size]
      (reify Function
        (apply [_ _key]
          (-> (badge-img type)
              (resize-image size)
              (write-to-bytes)))))))


(def cache-duration (.toSeconds TimeUnit/DAYS 1))

(def ^:private ^Cache compatibility-level-cache
  (-> (Caffeine/newBuilder)
      (.expireAfterWrite cache-duration TimeUnit/SECONDS)
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

(defn ^:private deck-compatibility-level
  "Returns the Steam Deck compatibility level for a given Steam app-id.
  (:verified, :playable, :unsupported, or :unknown)"
  [^long app-id]
  (if-not (> app-id 0)
    :unknown
    (let [app-id (long app-id)]
      (if-let [cached (.getIfPresent compatibility-level-cache app-id)]
        cached
        (let [level (find-deck-compatibility-level app-id)]
          (.put compatibility-level-cache app-id level)
          level)))))


(defn badge-for-app
  [app-id size]
  (-> (deck-compatibility-level app-id)
      (badge-bytes (or size 54))))



(comment

  (let [badge (badge-img :verified)]

    (write-to-file badge "target/testA.png")
    (write-to-file (resize-image badge 54) "target/testB.png"))

  (io/copy (badge-bytes :playable 54) (io/file "target/bytes.png"))

  ;
  )
