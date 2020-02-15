(ns beautify-text-notes.render
  (:require
    [clojure.string :as s]
    [hiccup.core :as h]
    [beautify-web.core :as bw]
    [selmer.parser :as sp]
    clojure.pprint))

(defn page 
  [{:keys [title]} rendered-children]
  [:section.page 
    [:div.page-title title]
    rendered-children])

(defn heading 
  [{:keys [text]} rendered-children]
  [:section.heading text 
    rendered-children])

(defn subheading 
  [{:keys [text]} rendered-children]
  [:section.subheading text 
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
    [:div.text-line text]
    [:div.inline-comment inline-comment]
    rendered-children])

(defn list-item 
  [{:keys [text symbol inline-comment]} rendered-children]
  [:div.list-item
    [:div.bullet symbol]
    [:div.list-text text]
    [:div.inline-comment inline-comment]
    rendered-children])

(defn invalid-line 
  [{:keys [text]}]
  [:pre.invalid-line text])

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
  [{:keys [type children plain-wrapper] :as structure}]
  (if (sequential? structure)
    (map create-hiccup structure)
    (let [render-fn (get render type)] 
       (if children
          (render-fn structure 
            (-> (create-hiccup children)
                (into 
                  (if plain-wrapper
                    [:div]
                    [:div.note-body]))
                (vec)))
          (render-fn structure)))))

;; REMOVE
(defn pt [x]
  (prn x)
  x)

(defn render-structure
  [structure notebook-name]
  (->> (create-hiccup structure)
       (h/html)
       (bw/beautify-html)
       (assoc {:notebook-name notebook-name
               :css           (slurp "./resources/content.css")} 
               :content)
       (sp/render-file "content.html")))
