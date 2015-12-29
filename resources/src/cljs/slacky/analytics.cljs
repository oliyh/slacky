(ns slacky.analytics)

(defn event! [event-name]
  (when js/window.ga
    (.ga js/window "send" "event" event-name)))
