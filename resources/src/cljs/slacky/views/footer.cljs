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
    ", to generate your memes, so anything you send to Slacky may end up there."]
   [:p "Users of the Slack integration will have any interaction transmitted via Slack's servers."]])

(defn component []
  [:div#footer
   [:p [:small "Contact "
        [:a {:href "https://twitter.com/oliyh"
             :target "_blank"} "@oliyh"]
        " or "
        [:a {:href "mailto://slackydev@gmail.com"}
         "slackydev@gmail.com"]]
    " / "
    [:small [:a {:href "https://github.com/oliyh/slacky"
                 :target "_blank"}
             "View on Github"]]
    " / "
    [:small [:a {:href "/privacy"
                 :on-click (nav! "/privacy")}
             "Privacy policy"]]]

   [:p [:small "Slacky is not created by, affiliated with, or supported by either Slack Technologies, Inc., Google or Mozilla"]]])
