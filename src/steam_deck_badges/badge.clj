(ns steam-deck-badges.badge
  (:require [clojure.java.io :as io])
  (:import (java.awt.geom AffineTransform)
           (java.awt.image AffineTransformOp BufferedImage)
           (java.io ByteArrayOutputStream)
           (java.util.concurrent ConcurrentHashMap)
           (java.util.function Function)
           (javax.imageio ImageIO)))

(defn ^:private new-image
  ^BufferedImage [w h]
  (BufferedImage. w h BufferedImage/TYPE_INT_ARGB))

(defn ^:private load-image
  ^BufferedImage [file]
  (ImageIO/read (io/file file)))


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


(def ^:private ^ConcurrentHashMap cache (ConcurrentHashMap.))

(defn badge-bytes
  "Returns PNG image bytes for a given badge compatibility type and size."
  [type size]
  (when (and (#{:verified :playable :unsupported :unknown} type)
             (<= 20 size 54))
    (.computeIfAbsent cache
      [type size]
      (reify Function
        (apply [_ _key]
          (-> (badge-img type)
              (resize-image size)
              (write-to-bytes)))))))



(comment

  (let [badge (badge-img :verified)]

    (write-to-file badge "target/testA.png")
    (write-to-file (resize-image badge 54) "target/testB.png"))

  (io/copy (badge-bytes :playable 54) (io/file "target/bytes.png"))

  ;
  )
