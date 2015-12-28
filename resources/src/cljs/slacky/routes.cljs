(ns slacky.routes
  (:require [goog.events :as events]
            [reagent.dom :as r]
            [secretary.core :as secretary :refer-macros [defroute]]
            [slacky.nav :as nav]
            [slacky.views.demo :as demo]
            [slacky.views.integrations :as integrations])
  (:import [goog.history Html5History EventType]))

(def app (js/document.getElementById "app"))

(defn- home-component []
  [:div
   [:div.avgrund-cover]
   [:div.avgrund-contents

    [:div.header
     [:h1 "Slacky"]
     [:h4 "Memes as a Service"]]

    [:div.row
     [:div.col-xs-10.col-xs-offset-1
      [demo/component]]]

    [:div.row
     [:div.col-xs-10.col-xs-offset-1
      [integrations/component (.getAttribute app "data-slack-oauth-url")]]]]])

(defn- modal-component [title content]
  (let [hide-modal (nav/nav! "/")]
    [:div
     [:div.slacky-modal-background
      [:div.slacky-modal-cover]]
     [:div.slacky-modal
      [:div
       [:button.close {:type "button"
                       :on-click hide-modal}
        [:span {:aria-hidden "true"} "×"]]
       [:h4 title]]
      [:div content]
      [:div
       [:button.btn.btn-default
        {:type "button"
         :on-click hide-modal}
        "Close"]]]]))

(defroute "/" {:as params}
  (r/render [home-component] app))

(defroute "/upgrade-slack" {:as params}
  (r/render [modal-component "Upgrade Slack"
             [integrations/upgrade-slack (.getAttribute app "data-slack-oauth-url")]] app))
