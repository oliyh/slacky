(ns slacky.service
  (:require [clojure.java.io :as io]
            [io.pedestal.http :as bootstrap]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.impl.interceptor :refer [terminate]]
            [io.pedestal.interceptor.helpers :refer [before handler]]
            [io.pedestal.interceptor :as interceptor]
            [pedestal.swagger.core :as swagger]
            [ring.util.codec :as codec]
            [ring.util.response :refer [response not-found created resource-response content-type status]]
            [schema.core :as s]
            [slacky
             [accounts :as accounts]
             [meme :as meme]
             [slack :as slack]
             [settings :as settings]]
            [clojure.tools.logging :as log]))

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

(s/defschema AddAccount
  {(req :token) s/Str
   (req :key)   s/Str})

;; api handlers

(swagger/defhandler meme
  {:summary "Process a Slack event"
   :parameters {:formData SlackRequest}
   :responses {200 {:schema s/Str}}}
  [{:keys [form-params] :as context}]
  (response
   (if (meme/valid-command? form-params)
     (do
       (log/info form-params)
       (future
         (try
           (meme/generate-meme form-params (slack/build-responder (::slack-webhook-url context) form-params))
           (catch Exception e (log/error e))))
       "Your meme is on its way")
     "Sorry, this is not a valid command syntax")))

(swagger/defhandler echo
  {:summary "Echoes a Slack event"
   :parameters {:formData SlackRequest}
   :responses {200 {:schema s/Any}}}
  [{:keys [form-params] :as req}]
  (response form-params))

(swagger/defhandler add-account
  {:summary "Adds an account"
   :parameters {:formData AddAccount}
   :responses {200 {:schema s/Any}}}
  [{:keys [form-params] :as req}]
  (try
    (accounts/add-account! (:db-connection req) (:token form-params) (:key form-params))
    (response "Account added")
    (catch Exception e
      (log/error "Failed to add account" e)
      (-> (response "Failed to add account")
          (status 500)))))


;; authentication

;; todo: lookup token / webhook pair in a data store
(swagger/defbefore authenticate-slack-call
  {:description "Ensures the caller has registered their token and an incoming webhook"
   :parameters {:formData {:token s/Str}}
   :responses {403 {}}}
  [{:keys [request response] :as context}]
  (let [db (:db-connection request)
        token (get-in request [:form-params :token])
        account (accounts/lookup-account db token)]
    (if-let [webhook-url (:key account)]
      (assoc-in context [:request ::slack-webhook-url] webhook-url)
      (-> context
          terminate
          (assoc-in [:response] {:status 403
                                 :headers {}
                                 :body (str "You are not permitted to use this service.\n"
                                            "Please register your token '"
                                            token
                                            "' at https://slacky-server.herokuapp.com")})))))

;; app-routes

(def home
  (handler ::home-handler
           (fn [req]
             (-> (resource-response "public/index.html")
                 (content-type "text/html")))))

;; routes

(s/with-fn-validation ;; Optional, but nice to have at compile time
  (swagger/defroutes api-routes
    {:info {:title "Slacky"
            :description "Memes and more for Slack"
            :externalDocs {:description "Find out more"
                           :url "https://github.com/oliyh/slacky"}
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

       ["/slack" ^:interceptors [authenticate-slack-call]
        ["/meme" ^:interceptors [(swagger/tag-route "meme")]
         {:post meme}]
        ["/echo" ^:interceptors [(swagger/tag-route "echo")]
         {:post echo}]]

       ["/account" ^:interceptors [(swagger/tag-route "account")]
        {:post add-account}]

       ["/doc" {:get [(swagger/swagger-doc)]}]
       ["/*resource" {:get [(swagger/swagger-ui)]}]]]]))

(defroutes app-routes
  [[["/" {:get home}]]])

(def routes
  (concat api-routes app-routes))

;; service

(def service
  {:env :prod
   ::bootstrap/routes routes
   ::bootstrap/router :linear-search
   ::bootstrap/resource-path "/public"
   ::bootstrap/type :jetty
   ::bootstrap/port (settings/web-port)})

(defn with-database [service db]
  (update-in service
             [::bootstrap/interceptors] conj
             (before ::inject-database
                     (fn [context]
                       (assoc-in context [:request :db-connection] db)))))
