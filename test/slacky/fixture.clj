(ns slacky.fixture
  (:require [clojure.core.async :as a]
            [io.pedestal.http :as bootstrap]
            [slacky
             [memecaptain :as memecaptain]
             [slack :as slack]
             [google :as google]
             [server :as server]
             [db :refer [create-fresh-db-connection]]]))

(def test-database-url "jdbc:sqlite:memory:test")

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



(defmacro with-fake-internet [{:keys [template-id meme-url search-result]
                               :or {template-id "b7k3me"
                                    meme-url (str memecaptain/memecaptain-url "/gend_images/a1jB3q.jpg")
                                    search-result "http://images.com/cat.jpg"}}
                              & body]
  `(let [slack-channel# (a/chan)]
     (cj/stubbing [memecaptain/create-template ~template-id
                   memecaptain/create-instance ~meme-url
                   google/image-search ~search-result
                   slack/send-message (fn [& args#]
                                        (a/put! slack-channel# args#))]

                  (let [~'template-id ~template-id
                        ~'meme-url ~meme-url
                        ~'search-result ~search-result
                        ~'slack-channel slack-channel#]
                    ~@body))))
