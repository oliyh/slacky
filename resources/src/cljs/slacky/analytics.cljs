(ns slacky.analytics)

(defn event! [event-name]
  (when js/ga
    (js/console.log "Recording event" event-name)
    (let [r (js/ga "send" "event" event-name)]
      (js/console.log "GA responded:" r))))
