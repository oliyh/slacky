(ns slacky.fixture
  (:require [clojure.core.async :as a]
            [io.pedestal.http :as bootstrap]
            [slacky
             [memecaptain :as memecaptain]
             [slack :as slack]
             [bing :as bing]
             [server :as server]
             [db :refer [create-fresh-db-connection]]]
            [slacky.settings :as settings]))

(def test-database-url "jdbc:h2:./db/test")

(def ^:dynamic *db* nil)
(defn with-database [f]
  (binding [*db* (create-fresh-db-connection test-database-url)]
    (f)))

(defn with-web-api [f]
  (when server/service-instance
    (bootstrap/stop server/service-instance))

  (server/create-server {:db-url test-database-url
                         :pedestal-opts {:env :dev
                                         ::bootstrap/join? false}})
  (bootstrap/start server/service-instance)

  (try (f)
       (finally (bootstrap/stop server/service-instance))))



(defmacro with-fake-internet [{:keys [template-id meme-file search-result slack-oauth-response]
                               :or {template-id "b7k3me"
                                    meme-file "/memes/ab342.jpg"
                                    search-result "http://images.com/cat.jpg"
                                    slack-oauth-response {:team-name "Team Name"
                                                          :team-id "team id"
                                                          :access-token "access-token"
                                                          :webhook-url "webhook-url"
                                                          :webhook-channel "webhook-channel"
                                                          :webhook-config-url "webhook-config-url"}}}
                              & body]
  `(let [slack-channel# (a/chan)
         meme-url# (str (settings/server-dns) "/" ~meme-file)]
     (cj/stubbing [memecaptain/create-direct ~meme-file
                   bing/image-search ~search-result
                   slack/send-message (fn [& args#]
                                        (a/put! slack-channel# args#))
                   slack/api-access ~slack-oauth-response]

                  (let [~'template-id ~template-id
                        ~'meme-file ~meme-file
                        ~'meme-url meme-url#
                        ~'search-result ~search-result
                        ~'slack-channel slack-channel#
                        ~'slack-oauth-response ~slack-oauth-response]
                    ~@body))))
