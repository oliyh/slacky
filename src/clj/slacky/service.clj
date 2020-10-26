(ns slacky.service
  (:require [angel.interceptor :as angel]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [io.pedestal.http :as bootstrap]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor.chain :refer [terminate]]
            [io.pedestal.interceptor.helpers :refer [before handler after]]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.ring-middlewares :as ring-middleware]
            [pedestal-api.core :as api]
            [ring.util.codec :as codec]
            [ring.util.response :refer [response not-found created resource-response content-type status redirect]]
            [schema.core :as s]
            [slacky
             [accounts :as accounts]
             [bing :as bing]
             [meme :as meme]
             [slack :as slack]
             [settings :as settings]]
            [slacky.views
             [index :as index]]
            [clojure.tools.logging :as log]
            [clojure.string :as string]))

;; schemas

(def req s/required-key)
(declare url-for)

(s/defschema MemeRequest
  {(req :text) s/Str})

;; api handlers

(defn- divert-to-slack [slack-responder result-channel]
  (a/go
    (let [[msg] (a/alts! [result-channel (a/timeout 180000)])]
      (if msg
        (apply slack-responder msg)
        (slack-responder :error "Sorry, your request timed out")))))

(def slack-meme
  (api/annotate
   {:summary "Responds asynchonously to a Slash command from Slack"
    :parameters {:form-params slack/SlackRequest}
    :responses {200 {:body (s/maybe s/Str)}}}
   (handler ::slack-meme
            (fn [{:keys [form-params] :as request}]
              (let [db (:db-connection request)
                    account-id (::account-id request)
                    text (:text form-params)
                    slack-responder (slack/build-responder (:response_url form-params) form-params)]
                (cond
                  (not (nil? (meme/resolve-template-addition text)))
                  (do (divert-to-slack slack-responder (meme/add-template db account-id text))
                      (response "Your template is being registered"))

                  (not (nil? (meme/resolve-meme-pattern db account-id text)))
                  (do (divert-to-slack slack-responder (meme/generate-meme db account-id text))
                      (response "Your meme is on its way"))

                  (re-matches #"(?i):help$" text)
                  (response (meme/generate-help db account-id))

                  (not (nil? (meme/resolve-template-deletion text)))
                  (do (divert-to-slack slack-responder (meme/delete-template db account-id text))
                      (response "Your template is being deleted"))

                  :else
                  (response "Sorry, the command was not recognised, try '/meme :help' for help")))))))

(def slack-image
  (api/annotate
   {:summary "Responds asynchonously to an image request from Slack"
    :parameters {:form-params slack/SlackRequest}
    :responses {:200 {:body s/Str}}}
   (handler ::slack-image
            (fn [{:keys [form-params]}]
              (let [text (:text form-params)
                    slack-responder (slack/build-responder (:response_url form-params) form-params)]

                (cond
                  (re-matches #"(?i):help$" text) (response "Type a search term e.g. \"pai mei\" or search for an animated image by typing \":anim pai mei\".")

                  :else
                  (do
                    (divert-to-slack slack-responder (let [response-chan (a/chan)]
                                                       (a/thread
                                                         (a/>!! response-chan [:image (bing/image-search text)]))
                                                       response-chan))
                    (response "Your image is on its way"))))))))

(def rest-meme
  (api/annotate
   {:summary "Responds synchronously with a meme"
    :parameters {:form-params MemeRequest}
    :responses {200 {:body s/Str}}}
   (handler ::rest-meme
            (fn [{:keys [form-params] :as request}]
              (let [db (:db-connection request)
                    account-id (::account-id request)
                    text (:text form-params)]

                (if (meme/resolve-meme-pattern db account-id text)
                  (let [response-chan (a/chan)
                        meme-chan (meme/generate-meme db account-id text)]

                    (a/go
                      (let [[[message-type msg]] (a/alts! [meme-chan (a/timeout 180000)])]
                        (if message-type
                          (if (= :error message-type)
                            (a/>! response-chan {:status 400
                                                 :body msg})
                            (a/>! response-chan (response msg)))
                          (a/>! response-chan {:status 504
                                               :body "Your request timed out"}))))

                    response-chan)
                  {:status 400
                   :body "Sorry, the command was not recognised - visit https://slacky-server.herokuapp.com to learn usage"}))))))

(def get-meme-patterns
  (api/annotate
   {:summary "Responds synchronously with meme patterns"
    :parameters {}
    :responses {200 {:body [{:pattern s/Str
                             (s/optional-key :template) s/Str}]}}}
   (handler ::get-meme-patterns
            (fn [_]
              {:status 200
               :body (meme/describe-meme-patterns)}))))

;; authentication

(def authenticate-basic-request
  (api/annotate
   {:description "Uses or creates a basic account"
    :parameters {:form-params {:token s/Str}}}
   (before ::authenticate-basic-request
           (fn [{:keys [request] :as context}]
             (log/info "Authenticating basic request...")
             (let [db (:db-connection request)
                   token (get-in request [:form-params :token])
                   account (or (accounts/lookup-basic-account db token)
                               (accounts/register-basic-account! db token))]
               (update context :request merge {::account-id (:id account)}))))))

(def authenticate-slack-request
  (api/annotate
   {:description "Uses or creates a Slack account, converting existing basic accounts to Slack accounts"
    :parameters {:form-params {:token s/Str
                               :team_id s/Str}}}
   (before ::authenticate-slack-request
           (fn [{:keys [request] :as context}]
             (log/info "Authenticating Slack request...")
             (let [db (:db-connection request)
                   form-params (:form-params request)
                   token (:token form-params)
                   team-id (:team_id form-params)]

               (if-let [account (accounts/lookup-slack-account db team-id)]
                 (update context :request merge {::account-id (:id account)})

                 (if-let [account (accounts/lookup-basic-account db token)]
                   (do (accounts/convert-to-slack-account! db (:id account) {:team-id team-id})
                       (update context :request merge {::account-id (:id account)}))

                   (let [account (accounts/register-slack-account! db {:team-id team-id})]
                     (update context :request merge {::account-id (:id account)})))))))))

(def slack-oauth
  (api/annotate
   {:description "Records the Slack credentials provided after successful authentication"
    :parameters {:query-params {(s/optional-key :code)  s/Str
                                (s/optional-key :error) s/Str
                                (s/optional-key :state) s/Str}}}
   (handler ::slack-oauth
            (fn [request]
              (if-let [error (get-in request [:query-params :error])]
                (do (log/error "User denied authentication")
                    (redirect (url-for ::home :path-params {:route "slack/denied"})))
                (if-let [api-access (slack/api-access
                                     (:slack-client-id request)
                                     (:slack-client-secret request)
                                     (get-in request [:query-params :code]))]
                  (do (accounts/register-slack-account!
                       (:db-connection request)
                       api-access)
                      (redirect (url-for ::home :path-params {:route "slack/success"})))
                  (redirect (url-for ::home :path-params {:route "slack/failure"}))))))))

;; usage stats

(def increment-api-usage
  (api/annotate
   {:description "Increments the api usage of this account"}
   (before ::increment-api-usage
           (fn [{:keys [request response] :as context}]
             (log/info "Incrementing API stats")
             (let [db (:db-connection request)
                   account-id (get request ::account-id)]
               (accounts/api-hit! db account-id))
             context))))

(def async-handler
  (api/annotate
   {:description "Wraps asynchronous responses into the context (lets you use defhandler)"}
   (after ::async-handler
          (fn [context]
            (if (satisfies? clojure.core.async.impl.protocols/Channel (:response context))
              (a/pipe (:response context) (a/chan 1 (map #(assoc context :response %))))
              context)))))

;; app-routes

(def meme-image
  (ring-middleware/file "."))

(def home
  (handler ::home-handler
           (fn [{:keys [google-analytics-key slack-client-id server-name]}]
             (-> (response (index/index {:google-analytics-key google-analytics-key
                                         :slack-oauth-url (format "https://slack.com/oauth/authorize?scope=incoming-webhook,commands&client_id=%s"
                                                                  slack-client-id)
                                         :app-name (if (= "zomg.oliy.co.uk" server-name)
                                                     "ZOMG"
                                                     "Slacky")}))
                 (content-type "text/html")))))

;; routes

(s/with-fn-validation ;; Optional, but nice to have at compile time
  (api/defroutes api-routes
    {:info {:title "Slacky"
            :description "Memes and more for Slack"
            :externalDocs {:description "Find out more"
                           :url "https://github.com/oliyh/slacky"}
            :version "2.0"}
     :tags [{:name "meme"
             :description "All the memes!"}
            {:name "image"
             :description "Image search"}]}
    [[["/api" ^:interceptors [api/error-responses
                              (api/negotiate-response)
                              (api/body-params)
                              api/common-body
                              (api/coerce-request)
                              (api/validate-response)
                              async-handler
                              (angel/prefers increment-api-usage :account)]

       ["/slack" ^:interceptors [(angel/provides authenticate-slack-request :account)]
        ["/meme" ^:interceptors [(api/doc {:tags ["meme"]})]
         {:post slack-meme}]
        ["/image" ^:interceptors [(api/doc {:tags ["image"]})]
         {:post slack-image}]]

       ["/oauth/slack"
        {:get slack-oauth}]

       ["/browser-plugin" ^:interceptors [(angel/provides authenticate-basic-request :account)]
        ["/meme" ^:interceptors [(api/doc {:tags ["meme"]})]
         {:post [::browser-plugin-meme rest-meme]}]]

       ["/meme" ^:interceptors [(api/doc {:tags ["meme"]})]
        {:post rest-meme}
        ["/patterns"
         {:get get-meme-patterns}]]

       ["/swagger.json" {:get api/swagger-json}]
       ["/*resource" {:get api/swagger-ui}]]]]))

(defroutes app-routes
  [[["/memes/:id" {:get meme-image}]
    ["/*route" {:get home}]]])

(def routes
  (concat api-routes app-routes))

(def url-for (route/url-for-routes routes))

;; service

(def service
  {:env :prod
   ::bootstrap/routes routes
   ::bootstrap/router :linear-search
   ::bootstrap/resource-path "/public"
   ::bootstrap/type :jetty
   ::bootstrap/port (settings/web-port)
   ::bootstrap/secure-headers {:content-security-policy-settings
                               {:script-src "'self' 'unsafe-inline' 'unsafe-eval'"}}})

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

(defn with-slack-client [service slack-client-id slack-client-secret]
  (update-in service
             [::bootstrap/interceptors] conj
             (before ::inject-slack-client-id
                     (fn [context]
                       (update context :request merge
                               {:slack-client-id slack-client-id
                                :slack-client-secret slack-client-secret})))))
