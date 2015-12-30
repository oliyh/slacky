(ns slacky.routes
  (:require [goog.events :as events]
            [reagent.dom :as r]
            [secretary.core :as secretary :refer-macros [defroute]]
            [slacky.nav :as nav]
            [slacky.views.demo :as demo]
            [slacky.views.integrations :as integrations]
            [slacky.views.footer :as footer])
  (:import [goog.history Html5History EventType]))

(def app (js/document.getElementById "app"))

(defn- home-component [& [modal]]
  [:div
   (when modal [:div.slacky-modal-cover])
   (when modal modal)
   [:div
    {:class (when modal "slacky-modal-blur")}
    [:div.header
     [:h1 "Slacky"]
     [:h4 "Memes as a Service"]]

    [:div.row
     [:div.col-xs-10.col-xs-offset-1
      [demo/component]]]

    [:div.row
     [:div.col-xs-10.col-xs-offset-1
      [integrations/component (.getAttribute app "data-slack-oauth-url")]]]

    [:div.row
     [:div.col-xs-10.col-xs-offset-1
      [footer/component]]]]])

(defn- modal-component [title content]
  (let [hide-modal (nav/nav! "/")]
    [:div.slacky-modal
     [:div
      [:button.close {:type "button"
                      :on-click hide-modal}
       [:span {:aria-hidden "true"} "×"]]
      [:h2 title]]
     [:div.slacky-modal-content content]
     [:div
      [:button.btn.btn-default
       {:type "button"
        :on-click hide-modal}
       "Close"]]]))

(defroute "/slack/upgrade" {:as params}
  (r/render [home-component [modal-component "Upgrade Slack"
                             [integrations/slack-upgrade (.getAttribute app "data-slack-oauth-url")]]] app))

(defroute "/slack/success" {:as params}
  (r/render [home-component [modal-component "Success!"
                             [integrations/slack-success]]] app))

(defroute "/slack/failure" {:as params}
  (r/render [home-component [modal-component "Failure!"
                             [integrations/slack-failure]]] app))

(defroute "/slack/denied" {:as params}
  (r/render [home-component [modal-component "Denied"
                             [integrations/slack-denied]]] app))

(defroute "/demo" {:as params}
  ;; todo use params to generate the meme
  (r/render [home-component [modal-component "Brace yourselves..."
                             [demo/create-meme]]] app))

(defroute "/privacy" {:as params}
  (r/render [home-component [modal-component "Privacy"
                             [footer/privacy]]] app))

(defroute "/" {:as params}
  (r/render [home-component] app))

(defroute "/*" {:as params}
  (r/render [home-component] app))