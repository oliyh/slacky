(ns slacky.settings)

(defn- rewrite-heroku-database-url [url]
  (condp re-find url
    #"^postgres://" ;; heroku
    (let [[_ user password host port database]
          (re-matches #"postgres://(.*):(.*)@(.*):(.*)/(.*)" url)]
      (format "jdbc:postgresql://%s:%s/%s?user=%s&password=%s"
              host port database user password))

    url))

(defn database-url []
  (or (when-let [provided-url (System/getenv "DATABASE_URL")]
        (rewrite-heroku-database-url provided-url))
      "jdbc:h2:./db/test"))

(defn web-port []
  (Integer. (or (System/getenv "PORT") 8080)))

(defn google-analytics-key []
  (System/getenv "GOOGLE_ANALYTICS_KEY"))

(defn slack-client-id []
  (System/getenv "SLACK_CLIENT_ID"))

(defn slack-client-secret []
  (System/getenv "SLACK_CLIENT_SECRET"))

(defn server-dns []
  (or (System/getenv "SERVER_DNS")
      (format "http://localhost:%s" (web-port))))
