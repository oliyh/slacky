(ns slemer.service
  (:require [pedestal.swagger.core :as swagger]
            [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.impl.interceptor :refer [terminate]]
            [ring.util.response :refer [response not-found created]]
            [ring.util.codec :as codec]
            [schema.core :as s]
            [slemer.meme :as meme]))

;; schemas

(s/defschema SlackRequest
  {:token        s/Str
   :team_id      s/Str
   :team_domain  s/Str
   :channel_id   s/Str
   :channel_name s/Str
   :user_id      s/Str
   :user_name    s/Str
   :command      s/Str
   :text         s/Str})

;; handlers

(swagger/defhandler meme
  {:summary "Process a Slack event"
   :parameters {:body SlackRequest}
   :responses {200 {:schema (s/maybe s/Str)}}}
  [{:keys [body-params]}]
  (response (meme/generate-meme body-params)))

;; routes

(s/with-fn-validation ;; Optional, but nice to have at compile time
  (swagger/defroutes routes
    {:info {:title "Slemer"
            :description "Memes and more for Slack"
            :version "2.0"}
     :tags [{:name "memes"
             :description "All the memes!"
             :externalDocs {:description "Find out more"
                            :url "https://github.com/oliyh/slemer"}}]}
    [[["/api" ^:interceptors [(body-params/body-params)
                              bootstrap/json-body
                              (swagger/body-params)
                              (swagger/keywordize-params :form-params :headers)
                              (swagger/coerce-params)
                              (swagger/validate-response)]

       ["/slack" ;; todo interceptor to check token is one we expect?
        ["/meme" ^:interceptors [(swagger/tag-route "meme")]
         {:post meme}]]

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
