(ns slacky.analytics)

(defn event! [event-name]
  (when js/ga
    (js/ga "send" "event" event-name)))
