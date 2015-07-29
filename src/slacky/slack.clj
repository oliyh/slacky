(ns slacky.slack
  (:require [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [schema.core :as s]))

(def req s/required-key)

(s/defschema SlackRequest
  {(req :token)        s/Str
   (req :team_id)      s/Str
   (req :team_domain)  s/Str
   (req :channel_id)   s/Str
   (req :channel_name) s/Str
   (req :user_id)      s/Str
   (req :user_name)    s/Str
   (req :command)      s/Str
   (req :text)         s/Str})

(def connection-pool (make-reusable-conn-manager {:timeout 10 :threads 4 :default-per-route 4}))

(defn meme-message [user-name meme-command meme-url]
  {:attachments
   [{:fallback (str user-name ": " meme-command)
     :pretext nil
     :color "#D00000"
     :title user-name
     :text (str meme-command "\n" meme-url)
     :image_url meme-url}]})

(defn- send-message [webhook-url channel slack-message]
  (let [message (merge (cond (map? slack-message)
                             slack-message

                             (string? slack-message)
                             {:text slack-message})
                       {:channel channel
                        :unfurl_links true})
        response (http/post webhook-url {:connection-manager connection-pool
                                         :body (json/encode message)
                                         :socket-timeout 2000
                                         :conn-timeout 2000})]
    (log/info "Sent message to" channel "and recieved response" (:status response))))

(defn build-responder [webhook-url {:keys [channel_name user_name text]}]
  (fn [destination meme-url]
    (try
      (send-message webhook-url
                    (get {:error (str "@" user_name)
                          :success (if (= "directmessage" channel_name)
                                     (str "@" user_name)
                                     (str "#" channel_name))}
                         destination)
                    (meme-message user_name text meme-url))
      (catch Exception e
        (log/warn "Could not send message to Slack" e)))))
