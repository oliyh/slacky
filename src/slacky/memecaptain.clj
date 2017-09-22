(ns slacky.memecaptain
  (:require [cheshire.core :as json]
            [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [clojure.core.memoize :as memo]
            [clojure.tools.logging :as log]))

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
