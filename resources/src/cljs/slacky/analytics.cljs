(ns slacky.analytics)

(defn event! [event-name]
  (js/console.log js/window.ga)
  (js/console.log js/ga)
  (js/console.log js/document.ga)

  (when (or js/window.ga js/ga js/document.ga)
    (js/console.log "Recording event" event-name)
    (let [r (.ga js/window "send" "event" event-name)]
      (js/console.log "GA responded:" r))))
