(ns slacky.memecaptain
  (:require [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.util UUID]
           [java.io File ByteArrayInputStream]
           [java.awt Font Color Graphics2D RenderingHints GraphicsEnvironment BasicStroke]
           [java.awt.font TextLayout FontRenderContext]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]))

(def ^:private impact-font (atom nil))

(defn init
  "Loads the Impact font and creates the meme directories"
  []
  (let [font-stream (io/input-stream (io/resource "memecaptain/impact.ttf"))
        font (Font/createFont Font/TRUETYPE_FONT font-stream)]
    (.registerFont (GraphicsEnvironment/getLocalGraphicsEnvironment) font)
    (reset! impact-font font))
  (.mkdir (io/file "./memes"))
  (.mkdir (io/file "./templates")))

(defn- get-font [size]
  (.deriveFont ^Font @impact-font (float size)))

(defn- fit-text-to-width
  "Find the largest font size that fits the text within max-width"
  [^Graphics2D g text max-width max-font-size min-font-size]
  (loop [size max-font-size]
    (if (<= size min-font-size)
      min-font-size
      (let [font (get-font size)
            metrics (.getFontMetrics g font)
            text-width (.stringWidth metrics text)]
        (if (<= text-width max-width)
          size
          (recur (- size 2)))))))

(defn- draw-meme-text
  "Draw text with white fill and black outline (classic meme style)"
  [^Graphics2D g ^String text img-width img-height position]
  (when-not (str/blank? text)
    (let [text (str/upper-case text)
          margin (int (* img-width 0.05))
          max-width (- img-width (* 2 margin))
          max-font-size (int (min 72 (/ img-height 6)))
          font-size (fit-text-to-width g text max-width max-font-size 12)
          font (get-font font-size)
          _ (.setFont g font)
          metrics (.getFontMetrics g)
          text-width (.stringWidth metrics text)
          text-height (.getHeight metrics)
          x (/ (- img-width text-width) 2)
          y (case position
              :top text-height
              :bottom (- img-height margin))
          stroke-width (max 2 (int (/ font-size 15)))]
      ;; Draw black outline using TextLayout for better quality
      (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
      (.setRenderingHint g RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
      (let [frc (.getFontRenderContext g)
            layout (TextLayout. text font frc)
            outline (.getOutline layout nil)
            transform (java.awt.geom.AffineTransform/getTranslateInstance x y)]
        (.setStroke g (BasicStroke. stroke-width BasicStroke/CAP_ROUND BasicStroke/JOIN_ROUND))
        (.setColor g Color/BLACK)
        (.draw g (.createTransformedShape transform outline))
        (.setColor g Color/WHITE)
        (.fill g (.createTransformedShape transform outline))))))

(defn- generate-meme-image
  "Generate a meme image with text overlay"
  [^BufferedImage source-image text-upper text-lower]
  (let [width (.getWidth source-image)
        height (.getHeight source-image)
        ;; Create a copy to avoid modifying the original
        result (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
        g (.createGraphics result)]
    (.drawImage g source-image 0 0 nil)
    (draw-meme-text g text-upper width height :top)
    (draw-meme-text g text-lower width height :bottom)
    (.dispose g)
    result))

(defn create-direct [image-url text-upper text-lower]
  (let [extension (or (second (re-find #".*\.(\w{3,4})($|\?)" image-url)) "jpg")
        filename (str (UUID/randomUUID) "." extension)
        output-file (io/file "memes/" filename)]
    (io/make-parents output-file)
    (log/info "Downloading" image-url)
    (let [response (http/get image-url {:as :byte-array})]
      (if-not (http/unexceptional-status? (:status response))
        (throw (ex-info (str "Could not download " image-url)
                        response))
        (let [source-image (ImageIO/read (ByteArrayInputStream. (:body response)))]
          (when (nil? source-image)
            (throw (ex-info "Could not decode image" {:image-url image-url})))
          (log/info "Generating meme" (.getPath output-file))
          (let [meme-image (generate-meme-image source-image text-upper text-lower)
                format (if (#{"png" "PNG"} extension) "png" "jpg")]
            (ImageIO/write meme-image format output-file)
            (.getPath output-file)))))))
