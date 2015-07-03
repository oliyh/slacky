(ns slemer.slack
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

(defn send-message [webhook-url channel-id text]
  (let [response (http/post webhook-url {:body (json/encode {:text text
                                                             :channel channel-id
                                                             :unfurl_links true})
                                         :socket-timeout 2000
                                         :conn-timeout 2000})]
    (log/info "Sent message to" channel-id "with response" (:status response))))
