(ns slacky.views.demo
  (:require [reagent.core :as r]
            [ajax.core :refer [POST GET]]
            [clojure.string :as string]
            [slacky.nav :refer [nav!]]
            [slacky.analytics :refer [event!]]))

(def meme-input (r/atom nil))
(def meme-output (r/atom nil))
(def meme-patterns (r/atom []))

(defn- classes [& classes]
  (string/join " " (remove nil? classes)))

(defn- example [img-src command]
  [:div.col-xs-12.col-md-4
   [:div.example {:on-click #(reset! meme-input command)}
    [:img.img-thumbnail.clickable {:src img-src}]
    [:code command]]])

(defn create-meme []
  ;; todo move the POST to here
  (when-let [{:keys [url command]}
             (and (not= :error @meme-output)
                  @meme-output)]
    [:div#meme-output
     [:img#demo-meme.center-block.img-thumbnail {:src (js/URL. url (.. js/window -location -href))}]
     [:br]
     [:code command]]))

(defn- generate-meme []
  ;; todo navigate to /demo/pai mei/pai mei/approves or something and let the handler do the POST
  (event! "demo")
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

(defn fetch-meme-patterns []
  (GET "/api/meme/patterns"
      {:response-format :json
       :keywords? true
       :handler #(reset! meme-patterns
                         (map
                          (fn [p]
                            (assoc p :pattern-tokens
                                   (-> (:pattern p)
                                       (string/replace #"\[([^\[]*)\]" "(.+)")
                                       (string/split " "))))
                          %))}))

(def meme-typeahead
  (with-meta
    (let [typeahead-focused? (r/atom false)]
      (fn [input-focused?]
        (when (or @input-focused?
                  @typeahead-focused?)
          [:div.tt-menu {:style {:position "absolute"
                                 :top "100%"
                                 :left 0
                                 :margin 0
                                 :z-index 100}}
           [:div
            {:on-mouseDown #(reset! typeahead-focused? true)
             :on-blur #(reset! typeahead-focused? false)}
            (map (fn [{:keys [pattern template]}]
                   [:div.typeahead-result.tt-suggestion.tt-selectable
                    {:key (or template "no-template")
                     :on-click #(do (reset! meme-input pattern)
                                    (reset! typeahead-focused? false)
                                    (.. js/document (getElementById "demo-text") focus))}
                    [:span pattern]
                    (when template
                      [:div.hidden-xs
                       [:img {:src template}]])])

                 (if (string/blank? @meme-input)
                   @meme-patterns
                   (let [query (some-> @meme-input string/trim (string/split " "))]
                     (filter
                      #(let [pattern (take (count query) (:pattern-tokens %))]
                         (loop [q query
                                p pattern]
                           (if (or (empty? q) (empty? p))
                             true

                             (if (or (re-find (re-pattern (first p)) (first q))
                                     (re-find (re-pattern (first q)) (first p)))
                               (recur (rest q) (rest p))
                               false))))
                      @meme-patterns))))]])))
    {:component-did-mount fetch-meme-patterns}))

(defn- meme-form []
  (let [focused? (r/atom false)]
    (fn []
      [:div#demo.form-horizontal
       [:div.col-xs-12.col-md-11
        [:div.form-group.form-group-lg
         [:div
          {:class (classes "input-group"
                           (when (= :error @meme-output) "has-error"))}
          [:div.input-group-addon "/meme"]
          [:input#demo-text.form-control {:type "text"
                                          :autoComplete "off"
                                          :value @meme-input
                                          :on-focus #(reset! focused? true)
                                          :on-blur #(reset! focused? false)
                                          :on-change #(reset! meme-input (-> % .-target .-value))
                                          :on-key-down #(case (.-which %)
                                                          13 (generate-meme)
                                                          27 (reset! meme-input nil)
                                                          nil)


                                          :placeholder "search term or url | upper text | lower text"}]
          [meme-typeahead focused?]]]]
       [:div.col-xs-12.col-md-1
        [:div.form-group.form-group-lg
         [:button.btn.btn-success.btn-lg
          {:on-click generate-meme}
          "Try!"]]]])))

(defn component [app-name]
  [:div.jumbotron
   [:a {:name "demo"}]
   [:div.leader
    [:h1 "How can haz meme?"]]

   [:div.row
    [example "/images/pai-mei-approves.png" "pai mei | pai mei | approves"]
    (if (= "ZOMG" app-name)
      [example "/images/zomg-win.png" "https://goo.gl/h9eUDM | zomg | win"]
      [example "/images/slacky-wins.png" "https://goo.gl/h9eUDM | slacky | wins"])
    [example "/images/all-the-memes.png" "create all the memes!"]]

   [:div.row
    [:div.col-xs-12
     [meme-form]]]])
