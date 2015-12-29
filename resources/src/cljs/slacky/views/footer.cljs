(ns slacky.views.footer
  (:require [slacky.nav :refer [nav!]]))

(defn- privacy []
  [:div
   [:p "Slacky stores a randomly generated token to identify your account, allowing
you to take advantage of various features."]
   [:p "Slacky uses a third-party service, "
    [:a {:href "http://memecaptain.com"
         :target "_blank"}
     "memecaptain.com"]
    ", to generate your memes, so anything you send to Slacky may end up there."]])

(defn component []
  [:div#footer
   [:p [:small "Contact "
        [:a {:href "https://twitter.com/oliyh"
             :target "_blank"} "@oliyh"]]
    " / "
    [:small [:a {:href "https://github.com/oliyh/slacky"
                 :target "_blank"}
             "View on Github"]]
    " / "
    [:small [:a {:href "/privacy"
                 :on-click (nav! "/privacy")}
             "Privacy policy"]]]])
