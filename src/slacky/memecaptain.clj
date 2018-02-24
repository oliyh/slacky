(ns slacky.memecaptain
  (:require [cheshire.core :as json]
            [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [clojure.core.memoize :as memo]
            [clojure.tools.logging :as log]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io])
  (:import [java.util UUID]
           [java.io File]))

(def ^:private one-hour-in-millis (* 60 60 1000))
(def memecaptain-url "http://memecaptain.com")
(def connection-pool (make-reusable-conn-manager {:timeout 10 :threads 4 :default-per-route 4}))

(defn- poll-for-result [polling-url]
  (log/debug "Polling" polling-url)
  (loop [attempts 0]
    (let [resp (http/get polling-url {:connection-manager connection-pool :redirect-strategy :none})
          status (:status resp)]

      (cond

        (= 303 status)
        (get-in resp [:headers "location"])

        (< 30 attempts)
        (throw (Exception. (str "Timed out waiting")))

        (= 200 status)
        (do (log/debug "Meme not ready, sleeping")
            (Thread/sleep 1000)
            (recur (inc attempts)))

        :else
        (throw (ex-info (str "Unexpected response from " polling-url)
                        resp))))))

(def create-template
  (memo/ttl
   (fn [image-url]
     (when image-url
       (log/info "Creating template for image" image-url)
       (let [resp (http/post (str memecaptain-url "/src_images")
                             {:connection-manager connection-pool
                              :headers {:Content-Type "application/json"
                                        :Accept "application/json"}
                              :redirect-strategy :none
                              :body (json/encode {:private true
                                                  :url (.trim image-url)})})]

         (if-let [polling-url (and (= 202 (:status resp))
                                   (not-empty (get-in resp [:headers "location"])))]

           (do (poll-for-result polling-url)
               (-> resp :body (json/decode keyword) :id))

           (throw (ex-info (str "Unexpected response from /src_images")
                           resp))))))
   :ttl/threshold one-hour-in-millis))

(defn create-instance [template-id text-upper text-lower]
  (log/info "Generating meme based on template" template-id)
  (let [resp (http/post (str memecaptain-url "/gend_images")
                        {:connection-manager connection-pool
                         :headers {:Content-Type "application/json"
                                   :Accept "application/json"}
                         :redirect-strategy :none
                         :body (json/encode {:private true
                                             :src_image_id template-id
                                             :captions_attributes
                                             [{:text text-upper
                                               :top_left_x_pct 0.05
                                               :top_left_y_pct 0
                                               :width_pct 0.9
                                               :height_pct 0.25}
                                              {:text text-lower
                                               :top_left_x_pct 0.05
                                               :top_left_y_pct 0.75
                                               :width_pct 0.9
                                               :height_pct 0.25}]})})]

    (if-let [polling-url (and (= 202 (:status resp))
                              (not-empty (get-in resp [:headers "location"])))]

      (poll-for-result polling-url)

      (throw (ex-info (str "Unexpected response")
                      resp)))))

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
