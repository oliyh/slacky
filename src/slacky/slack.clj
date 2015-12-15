(ns slacky.slack
  (:require [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [schema.core :as s]))

(def req s/required-key)
(def opt s/optional-key)

(s/defschema SlackRequest
  {(req :token)        s/Str
   (req :team_id)      s/Str
   (req :team_domain)  s/Str
   (req :channel_id)   s/Str
   (req :channel_name) s/Str
   (req :user_id)      s/Str
   (req :user_name)    s/Str
   (req :command)      s/Str
   (req :text)         s/Str
   (req :response_url) s/Str})

(def connection-pool (make-reusable-conn-manager {:timeout 10 :threads 4 :default-per-route 4}))

(defmulti ->message (fn [message-type & args] message-type))

(defmethod ->message :meme [_ user-name meme-command meme-url]
  {:attachments
   [{:fallback (str user-name ": " meme-command)
     :pretext nil
     :color "#D00000"
     :title user-name
     :text (str meme-command "\n" meme-url)
     :image_url meme-url}]})

(defmethod ->message :add-template [_ user-name _ name image-url]
  {:attachments
   [{:fallback (format "%s added a template called '%s' for %s" user-name name image-url)
     :pretext nil
     :color "#D00000"
     :title (format "%s added a meme template called '%s'" user-name name)
     :image_url image-url}]})

(defmethod ->message :default [_ _ _ string-message]
  string-message)

(defn send-message [webhook-url response-type slack-message]
  (let [message (merge (cond (map? slack-message)
                             slack-message

                             (string? slack-message)
                             {:text slack-message})
                       {:response_type response-type
                        :unfurl_links true})
        response (http/post webhook-url {:connection-manager connection-pool
                                         :body (json/encode message)
                                         :socket-timeout 2000
                                         :conn-timeout 2000})]
    (log/info "Sent message to" response-type "and recieved response" (:status response))))

(defn build-responder [webhook-url {:keys [channel_name user_name text]}]
  (fn [message-type & args]
    (try
      (send-message webhook-url
                    (get {:add-template "in_channel"
                          :meme (if (= "directmessage" channel_name)
                                  "ephemeral"
                                  "in_channel")
                          :help "ephemeral"}
                         message-type
                         "ephemeral")
                    (apply ->message message-type user_name text args))
      (catch Exception e
        (log/warn "Could not send message to Slack" e)))))

(defn register-application [db client-id client-secret oauth-code]
  (let [result (-> (http/post "https://slack.com/api/oauth.access"
                              {:form-params {:client-id client-id
                                             :client-secret client-secret
                                             :code oauth-code}})
                   :body
                   (json/decode true))]
    (log/info "Params for oauth:" {:form-params {:client-id client-id
                                                 :client-secret client-secret
                                                 :code oauth-code}})
    (log/info result)
    (true? (:ok result))))
