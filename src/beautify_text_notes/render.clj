(ns beautify-text-notes.render
  (:require
    [clojure.string :as s]
    [hiccup.core :as h]
    [beautify-web.core :as bw]
    [selmer.parser :as sp]
    clojure.pprint))

(defn blank
  [_]
  [:br])

(defn invalid-line 
  [{:keys [text]}]
  [:pre.invalid-line text])

(defn determine-class-level 
  [rendered-children level]
  ;; TODO probably shouldn't determine it like this?
  (if (some #(= [:br] %) 
            (rest rendered-children)) 
    (str "level-" level)
    "leaf-text"))

(defn page 
  [{:keys [title]} rendered-children & [level]]
  [:section.page 
    [:div.page-title title]
    rendered-children])

(defn heading 
  [{:keys [text]} rendered-children level]
  [:section.heading
    [:span {:class (str "level-" level)} text] 
    rendered-children])

(defn subheading 
  [{:keys [text]} rendered-children level]
  [:section.subheading  
    [:span {:class (str "level-" level)} text] 
    rendered-children])

(defn divider
  [_ rendered-children level]
  [:div 
    [:hr {:class (str "level-" level)}]
    rendered-children])

(defn text-line
  [{:keys [text inline-comment]} rendered-children level]
  [:div {:class (determine-class-level rendered-children level)}
    [:div.line 
      [:div.text-line text]
      [:div.inline-comment inline-comment]]
    rendered-children])

(defn list-item 
  [{:keys [text symbol inline-comment]} rendered-children level]
  [:div {:class (determine-class-level rendered-children level)}
    [:div.list-item
      [:div.bullet symbol]
      [:div.list-text text]
      [:div.inline-comment inline-comment]]
    rendered-children])

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
  ([structure]
    (create-hiccup structure 0))
  ([{:keys [type children plain-wrapper] :as structure} level]
    (if (sequential? structure)
      (map #(create-hiccup % level) structure)
      (let [render-fn (get render type)] 
         (if children
            (render-fn structure 
              (-> (create-hiccup children (inc level))
                  (into 
                    (if plain-wrapper
                      [:div]
                      [:div.note-body]))
                  (vec))
              level)
            (render-fn structure))))))

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
               :css           (slurp "./resources/content.css")
               :time-stamp    (.toString (java.util.Date.))
              } 
               :content)
       (sp/render-file "content.html")))
