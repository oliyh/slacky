(ns slacky.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events])
  (:import goog.History
           goog.history.EventType))

(secretary/set-config! :prefix "#")

(defroute "/demo" {:as params}
  (println "Demoing!"))

(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
