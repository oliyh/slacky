(ns slacky.server
  (:gen-class) ; for -main method in uberjar
  (:require [slacky
             [db :as db]
             [service :as service]
             [settings :as settings]]
            [io.pedestal.http :as bootstrap]))

(defonce service-instance nil)

(defn create-server
  "Standalone dev/prod mode."
  ([] (create-server {}))
  ([{:keys [db-url pedestal-opts]}]
   (alter-var-root #'service-instance
                   (constantly (bootstrap/create-server
                                (-> (merge service/service pedestal-opts)
                                    (bootstrap/default-interceptors)
                                    (service/with-database (db/create-db-connection (or db-url (settings/database-url))))))))))

(defn -main [& args]
  (create-server)
  (bootstrap/start service-instance))

(comment
  ;; Container prod mode for use with the io.pedestal.servlet.ClojureVarServlet class.

  (defn servlet-init [this config]
    (alter-var-root #'service-instance
                    (constantly (bootstrap/create-servlet service/service)))
    (.init (::bootstrap/servlet service-instance) config))

  (defn servlet-destroy [this]
    (alter-var-root #'service-instance nil))

  (defn servlet-service [this servlet-req servlet-resp]
    (.service ^javax.servlet.Servlet (::bootstrap/servlet service-instance)
              servlet-req servlet-resp)))
