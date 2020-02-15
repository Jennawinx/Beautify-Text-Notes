(ns beautify-text-notes.render
  (:require
    [clojure.string :as s]
    [hiccup.core :as h]
    [beautify-web.core :as bw]
    clojure.pprint))

(defn page 
  [{:keys [title]} rendered-children]
  [:div.page 
    [:div.page-title title]
    rendered-children])

(defn heading 
  [{:keys [text]} rendered-children]
  [:div.heading text 
    rendered-children])

(defn subheading 
  [{:keys [text]} rendered-children]
  [:div.subheading text 
    rendered-children])

(defn divider
  [_ rendered-children]
  [:div 
    [:hr]
    rendered-children])

(defn blank
  [_]
  [:br])

(defn text-line
  [{:keys [text inline-comment]} rendered-children]
  [:div.line 
    [:span.text-line text]
    [:span.inline-comment inline-comment]
    rendered-children])

(defn list-item 
  [{:keys [text symbol inline-comment]} rendered-children]
  [:div.list-item
    [:span.bullet symbol]
    [:span.list-text text]
    [:span.inline-comment inline-comment]
    rendered-children])

(defn invalid-line 
  [{:keys [text]}]
  [:span.invalid-line text])

(def render 
  {:invalid-line invalid-line
   :blank        blank
   :subheading   subheading
   :heading      heading
   :divider      divider
   :text-line    text-line
   :list-item    list-item
   :page         page})

(defn create-hiccup 
  [{:keys [type children] :as structure}]
  (if (sequential? structure)
    (-> (map create-hiccup structure)
        (into [:div.note-body])
        (vec))
    (let [render-fn (get render type)] 
       (if children
          (render-fn structure (create-hiccup children))
          (render-fn structure)))))

(defn render-structure
  [structure]
  (-> (create-hiccup structure)
      (h/html)
      ;; templating from here?
      (bw/beautify-html)
      ))
