(ns slacky.views.demo
  (:require [reagent.core :as r]
            [ajax.core :refer [POST]]
            [clojure.string :as string]
            [slacky.nav :refer [nav!]]))

(def meme-input (r/atom nil))
(def meme-output (r/atom nil))

(defn- classes [& classes]
  (string/join " " (remove nil? classes)))

(defn- example [img-src command]
  [:div.col-xs-12.col-md-4
   [:div.example {:on-click #(reset! meme-input command)}
    [:img.img-thumbnail {:src img-src}]
    [:code command]]])

(defn create-meme []
  ;; todo move the POST to here
  (when-let [{:keys [url command]}
             (and (not= :error @meme-output)
                  @meme-output)]
    [:div#meme-output
     [:img#demo-meme.center-block.img-thumbnail {:src url}]
     [:br]
     [:code command]]))

(defn- generate-meme []
  ;; todo navigate to /demo/pai mei/pai mei/approves or something and let the handler do the POST
  (let [command @meme-input]
    (reset! meme-output {:url "/images/loading.gif"
                         :command command})
    (POST "/api/meme"
        {:format :raw
         :params {:text @meme-input}
         :handler #(reset! meme-output {:url %
                                        :command command})
         :error-handler #(do (reset! meme-output :error)
                             ((nav! "/")))})
    ((nav! "/demo"))))

(defn- meme-form []
  [:div#demo.form-horizontal
   [:div.col-xs-12.col-md-11
    [:div.form-group.form-group-lg
     [:div
      {:class (classes "input-group"
                       (when (= :error @meme-output) "has-error"))}
      [:div.input-group-addon "/meme"]
      [:input#demo-text.form-control {:type "text"
                                      :value @meme-input
                                      :on-change #(reset! meme-input (-> % .-target .-value))
                                      :on-key-down #(case (.-which %)
                                                      13 (generate-meme)
                                                      27 (reset! meme-input nil)
                                                      nil)
                                      :placeholder "search term or url | upper text | lower text"}]]]]
   [:div.col-xs-12.col-md-1
    [:div.form-group.form-group-lg
     [:button.btn.btn-success.btn-lg
      {:on-click generate-meme}
      "Try!"]]]])

(defn component []
  [:div.jumbotron
   [:a {:name "demo"}]
   [:div.leader
    [:h1 "How can haz meme?"]]

   [:div.row
    [example "/images/pai-mei-approves.png" "pai mei | pai mei | approves"]
    [example "/images/slacky-wins.png" "https://goo.gl/h9eUDM | slacky | wins"]
    [example "/images/all-the-memes.png" "create all the memes!"]]

   [:div.row
    [:div.col-xs-12
     [meme-form]]]])
