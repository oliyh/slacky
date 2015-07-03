(ns slemer.service
  (:require [clojure.java.io :as io]
            [io.pedestal.http :as bootstrap]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.impl.interceptor :refer [terminate]]
            [pedestal.swagger.core :as swagger]
            [ring.util.codec :as codec]
            [ring.util.response :refer [response not-found created]]
            [schema.core :as s]
            [slemer
             [meme :as meme]
             [slack :as slack]]
            [clojure.tools.logging :as log]))

;; settings
(def slack-webhook-url
  (or (System/getenv "WEBHOOK-URL")
      (when-let [f (io/resource "webhook-url.txt")]
        (.trim (slurp f)))))

;; schemas

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

;; handlers

(swagger/defhandler meme
  {:summary "Process a Slack event"
   :parameters {:formData SlackRequest}
   :responses {200 {:schema s/Str}}}
  [{:keys [form-params]}]
  (response (do (future
                  (try
                    (slack/send-message slack-webhook-url
                                        (:channel_id form-params)
                                        (meme/generate-meme form-params))
                    (catch Exception e (log/error e))))
                "OK")))

(swagger/defhandler echo
  {:summary "Echoes a Slack event"
   :parameters {:formData SlackRequest}
   :responses {200 {:schema s/Any}}}
  [{:keys [form-params]}]
  (response form-params))

(swagger/defhandler echo-text
  {:summary "Echoes a Slack event"
   :parameters {:formData SlackRequest}
   :responses {200 {:schema s/Any}}}
  [{:keys [form-params]}]
  (response (:text form-params)))

(swagger/defhandler echo-delay
  {:summary "Echoes a Slack event after a delay"
   :parameters {:formData SlackRequest}
   :responses {200 {:schema s/Any}}}
  [{:keys [form-params]}]
  (response (do (Thread/sleep (Long/parseLong (:text form-params)))
                "https://i.imgflip.com/nqg70.jpg")))

;; routes

(s/with-fn-validation ;; Optional, but nice to have at compile time
  (swagger/defroutes routes
    {:info {:title "Slemer"
            :description "Memes and more for Slack"
            :externalDocs {:description "Find out more"
                           :url "https://github.com/oliyh/slemer"}
            :version "2.0"}
     :tags [{:name "memes"
             :description "All the memes!"}
            {:name "echo"
             :description "Echoes content back"}]}
    [[["/api" ^:interceptors [(body-params/body-params)
                              bootstrap/json-body
                              (swagger/body-params)
                              (swagger/keywordize-params :form-params :headers)
                              (swagger/coerce-params)
                              (swagger/validate-response)]

       ["/slack" ;; todo interceptor to check token is one we expect?
        ["/meme" ^:interceptors [(swagger/tag-route "meme")]
         {:post meme}]
        ["/echo" ^:interceptors [(swagger/tag-route "echo")]
         {:post echo}]
        ["/echo-text" ^:interceptors [(swagger/tag-route "echo")]
         {:post echo-text}]
        ["/echo-delay" ^:interceptors [(swagger/tag-route "echo")]
         {:post echo-delay}]]

       ["/doc" {:get [(swagger/swagger-doc)]}]
       ["/*resource" {:get [(swagger/swagger-ui)]}]]]]))

;; service

(def port (Integer. (or (System/getenv "PORT") 8080)))

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/router :linear-search
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port port})
