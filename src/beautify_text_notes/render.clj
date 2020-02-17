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
  [{:keys [title]} _]
  [:section.page 
    [:div.page-title title]])

(defn heading 
  [{:keys [text]} leveltag]
  [:section.heading
    [:div.heading-title {:class {:class leveltag}} text]])

(defn subheading 
  [{:keys [text]} leveltag]
  [:section.subheading  
    [:div.subheading-title {:class {:class leveltag}} text]])

(defn divider
  [_ leveltag]
  [:hr {:class leveltag}])

(defn text-line
  [{:keys [text inline-comment]} leveltag]
  [:div.line-group
    [:div.line {:class {:class leveltag}}
      [:div.text-line text]
      [:div.inline-comment inline-comment]]])

(defn list-item 
  [{:keys [text symbol inline-comment]} leveltag]
  [:div.list-group 
    [:div.list-item {:class leveltag}
      [:div.bullet symbol]
      [:div.list-text text]
      [:div.inline-comment inline-comment]]])

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
            (let [child-level       (inc level)
                  rendered-children (-> (create-hiccup children child-level)
                                        (into 
                                          (if plain-wrapper
                                            [:div]
                                            [:div.note-body]))
                                        (vec))
                  level-tag         (determine-class-level rendered-children child-level)]
            (concat 
              (render-fn structure level-tag)
              rendered-children))
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
