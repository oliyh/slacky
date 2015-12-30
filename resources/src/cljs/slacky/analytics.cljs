(ns slacky.analytics)

(defn event! [event-name]
  (js/console.log js/window.ga)
  (when js/window.ga
    (js/console.log "Recording event" event-name)
    (let [r (.ga js/window "send" "event" event-name)]
      (js/console.log "GA responded:" r))))
