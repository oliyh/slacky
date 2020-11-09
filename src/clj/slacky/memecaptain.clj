(ns slacky.memecaptain
  (:require [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io])
  (:import [java.util UUID]
           [java.io File]))

(defn init
  "Copies the memecaptain binary to disk so it can be called"
  []
  (io/copy (io/input-stream (io/resource "memecaptain/memecaptain"))
           (io/file "memecaptain"))
  (sh/sh "chmod" "+x" "memecaptain")
  (io/copy (io/input-stream (io/resource "memecaptain/impact.ttf"))
           (io/file "impact.ttf"))
  (.mkdir (io/file "./memes"))
  (.mkdir (io/file "./templates")))

(defn create-direct [image-url text-upper text-lower]
  (let [extension (second (re-find #".*\.(\w{3,4})($|\?)" image-url))
        filename (str (UUID/randomUUID) (when extension (str "." extension)))
        output-file (io/file "memes/" filename)
        input-file (io/file "templates/" filename)]
    (io/make-parents input-file)
    (io/make-parents output-file)
    (log/info "Downloading" image-url "to" (.getPath input-file))
    (try
      (let [response (http/get image-url {:as :byte-array})]
        (if-not (http/unexceptional-status? (:status response))
          (throw (ex-info (str "Could not download" image-url)
                          response))
          (do (io/copy (io/input-stream (:body response)) input-file)
              (log/info "Generating meme" (.getPath output-file))
              (sh/with-sh-dir (io/file ".")
                (let [result (sh/sh "./memecaptain" (.getAbsolutePath input-file) "-o" (.getAbsolutePath output-file) "-f" "impact.ttf" "-t" text-upper "-b" text-lower)]
                  (if (zero? (:exit result))
                    (.getPath output-file)
                    (throw (ex-info "Failed to generate meme"
                                    (merge result
                                           {:image-url image-url
                                            :input-file (.getPath input-file)
                                            :output-file (.getPath output-file)})))))))))
      (finally
        (.delete input-file)))))
