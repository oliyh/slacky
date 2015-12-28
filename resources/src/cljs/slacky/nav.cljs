(ns slacky.nav
  (:require [goog.events :as events]
            [secretary.core :as secretary])
  (:import [goog.history Html5History EventType]))

(defonce history
  (let [h (Html5History.)]
    (goog.events/listen h EventType.NAVIGATE #(secretary/dispatch! (.-token %)))
    (doto h (.setEnabled true))))

(defn nav! [token]
  (.setToken history token))

(defn current-path []
  (if (Html5History.isSupported)
    (str js/window.location.pathname js/window.location.search)
    (if (= js/window.location.pathname "/")
      (.substring js/window.location.hash 1)
      (str js/window.location.pathname js/window.location.search))))
