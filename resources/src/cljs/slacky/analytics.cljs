(ns slacky.analytics)

(defn event! [event-name]
  (when (exists? js/ga)
    (js/ga "send" "event" event-name)))
