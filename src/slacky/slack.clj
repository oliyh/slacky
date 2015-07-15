(ns slacky.slack
  (:require [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

(def connection-pool (make-reusable-conn-manager {:timeout 10 :threads 4 :default-per-route 4}))

(defn send-message [webhook-url {:keys [channel_id text user_name]} message]
  (let [response (http/post webhook-url {:connection-manager connection-pool
                                         :body (json/encode {:text message
                                                             :attachments
                                                             [{:fallback (str user_name ": " text)
                                                               :pretext nil
                                                               :color "#D00000"
                                                               :fields [{:title user_name
                                                                         :value text
                                                                         :short false}]}]
                                                             :channel channel_id
                                                             :unfurl_links true})
                                         :socket-timeout 2000
                                         :conn-timeout 2000})]
    (log/info "Sent message to" channel_id "with response" (:status response))))
