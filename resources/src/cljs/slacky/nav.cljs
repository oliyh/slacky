(ns slacky.nav
  (:require [goog.events :as events]
            [secretary.core :as secretary])
  (:import [goog.history Html5History EventType]
           [goog History]))

;; taken from http://www.lispcast.com/mastering-client-side-routing-with-secretary-and-goog-history

(aset js/goog.history.Html5History.prototype "getUrl_"
      (fn [token]
        (this-as this
                 (if (.-useFragment_ this)
                   (str "#" token)
                   (str (.-pathPrefix_ this) token)))))

(defn get-token []
  (if (Html5History.isSupported)
    (str js/window.location.pathname js/window.location.search)
    (if (= js/window.location.pathname "/")
      (.substring js/window.location.hash 1)
      (str js/window.location.pathname js/window.location.search))))

(defn- make-history []
  (if (Html5History.isSupported)
    (doto (Html5History.)
      (.setPathPrefix (str js/window.location.protocol
                           "//"
                           js/window.location.host))
      (.setUseFragment false))
    (if (not= "/" js/window.location.pathname)
      (aset js/window "location" (str "/#" (get-token)))
      (History.))))

(defn handle-url-change [e]
  (js/console.log (str "Navigating: " (get-token)))
  (when-not (.-isNavigation e)
    (js/window.scrollTo 0 0))
  (secretary/dispatch! (get-token)))

(defonce history (doto (make-history)
                   (goog.events/listen EventType.NAVIGATE #(handle-url-change %))
                   (.setEnabled true)))

(defn nav! [token]
  (fn [event] (do (when event (.preventDefault event))
                  (.setToken history token))))
