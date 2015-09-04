(ns slacky.service
  (:require [angel.interceptor :as angel]
            [clojure.java.io :as io]
            [io.pedestal.http :as bootstrap]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.impl.interceptor :refer [terminate]]
            [io.pedestal.interceptor.helpers :refer [before handler]]
            [io.pedestal.interceptor :as interceptor]
            [pedestal.swagger.core :as swagger]
            [pedestal.swagger.doc :as sw.doc]
            [ring.util.codec :as codec]
            [ring.util.response :refer [response not-found created resource-response content-type status]]
            [schema.core :as s]
            [slacky
             [accounts :as accounts]
             [meme :as meme]
             [slack :as slack]
             [settings :as settings]]
            [slacky.views
             [index :as index]]
            [clojure.tools.logging :as log]
            [clojure.string :as string]))

;; schemas

(def req s/required-key)

(s/defschema MemeRequest
  {(req :text) s/Str})

(s/defschema AddAccount
  {(req :token) (s/both s/Str
                        (s/named (s/pred #(<= 20 (count %))) "must be at least 20 characters long")
                        (s/named (s/pred #(re-matches #"\w*" %)) "must be alphanumeric"))
   (req :key)   (s/both s/Str
                        (s/named (s/pred #(re-matches #"^https://hooks\.slack\.com/services/.+" %))
                                 "must be of the form https://hooks.slack.com/services/..."))})

;; api handlers

(swagger/defhandler slack-meme
  {:summary "Responds asynchonously with a meme to a Slash command from Slack"
   :parameters {:formData slack/SlackRequest}
   :responses {200 {:schema s/Str}}}
  [{:keys [form-params] :as request}]
  (try
    {:status 200
     :body (meme/handle-request (:db-connection request)
                                (::account-id request)
                                (:text form-params)
                                (slack/build-responder (::slack-webhook-url request) form-params))}

    (catch IllegalArgumentException e
      {:status 200
       :body (.getMessage e)})

    (catch Exception e
      (log/error e)
      {:status 500
       :body "Something went wrong, check my logs"})))

(swagger/defhandler rest-meme
  {:summary "Responds synchronously with a meme"
   :parameters {:formData MemeRequest}
   :responses {200 {:schema s/Str}}}
  [{:keys [form-params] :as request}]

  (try
    (let [response-promise (promise)]
      (meme/handle-request (:db-connection request)
                           (::account-id request)
                           (:text form-params)
                           (fn [destination meme-url]
                             (deliver response-promise
                                      (condp = destination
                                        :error {:status 500
                                                :body meme-url}
                                        :success (response meme-url)))))

      (if-let [response (deref response-promise 180000 false)]
        response
        {:status 504
         :body "Your request timed out"}))

     (catch IllegalArgumentException e
       {:status 400
        :body (.getMessage e)})

     (catch Exception e
       (log/error e)
       {:status 500
        :body "Something went wrong, check my logs"})))

(swagger/defhandler get-meme-patterns
  {:summary "Responds synchronously with a meme"
   :responses {200 {:schema [{:pattern s/Str
                              (s/optional-key :template) s/Str}]}}}
  [context]
  {:status 200
   :body (meme/describe-meme-patterns)})

(swagger/defhandler add-account
  {:summary "Adds an account"
   :parameters {:formData AddAccount}
   :responses {200 {:schema s/Any}
               409 {:schema s/Str}}}
  [{:keys [form-params] :as req}]
  (let [db (:db-connection req)]
    (try
      (if (accounts/lookup-account db (:token form-params))
        (-> (response "Account already exists")
            (status 409))
        (do (accounts/add-account! db (:token form-params) (:key form-params))
            (response "Account added")))
      (catch Exception e
        (log/error "Failed to add account" e)
        (-> (response "Failed to add account")
            (status 500))))))


;; authentication

(swagger/defbefore authenticate-slack-call
  {:description "Ensures the caller has registered their token and an incoming webhook"
   :parameters {:formData {:token s/Str}}
   :responses {403 {}}}
  [{:keys [request response] :as context}]
  (log/info "Authenticating")
  (let [db (:db-connection request)
        token (get-in request [:form-params :token])
        account (accounts/lookup-account db token)]
    (if-let [webhook-url (:key account)]
      (update context :request merge {::slack-webhook-url webhook-url
                                      ::account-id (:id account)})
      (-> context
          terminate
          (assoc-in [:response] {:status 403
                                 :headers {}
                                 :body (str "You are not permitted to use this service.\n"
                                            "Please register your token '"
                                            token
                                            "' at https://slacky-server.herokuapp.com")})))))

;; usage stats

(swagger/defbefore increment-api-usage
  {:description "Increments the api usage of this account"}
  [{:keys [request response] :as context}]
  (log/info "Incrementing API stats")
  (let [db (:db-connection request)
        account-id (get request ::account-id)]
    (accounts/api-hit! db account-id))
  context)

(defn- annotate "Adds metatata m to a swagger route" [m]
  (sw.doc/annotate m (before ::annotate identity)))

;; app-routes

(def home
  (handler ::home-handler
           (fn [{:keys [google-analytics-key]}]
             (-> (response (index/index {:google-analytics-key google-analytics-key
                                         :meme-descriptions (meme/describe-meme-patterns)}))
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
             :description "All the memes!"}]}
    [[["/api" ^:interceptors [(swagger/body-params)
                              bootstrap/json-body
                              (swagger/coerce-request)
                              (swagger/validate-response)
                              (angel/prefers increment-api-usage :account)]

       ["/slack" ^:interceptors [(angel/provides authenticate-slack-call :account)]
        ["/meme" ^:interceptors [(annotate {:tags ["meme"]})]
         {:post slack-meme}]]

       ["/meme" ^:interceptors [(annotate {:tags ["meme"]})]
        {:post rest-meme}
        ["/patterns"
         {:get get-meme-patterns}]]

       ["/account" ^:interceptors [(annotate {:tags ["account"]})]
        {:post add-account}]

       ["/swagger.json" {:get [(swagger/swagger-json)]}]
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

(defn with-google-analytics [service google-analytics-key]
  (update-in service
             [::bootstrap/interceptors] conj
             (before ::inject-google-analytics-key
                     (fn [context]
                       (assoc-in context [:request :google-analytics-key] google-analytics-key)))))
