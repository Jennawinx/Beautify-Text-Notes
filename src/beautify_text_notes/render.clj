(ns beautify-text-notes.render
  (:require
    [clojure.string :as s]
    clojure.pprint))

(defn blank
  [_]
  [:div.blank])

(defn invalid-line 
  [{:keys [text]}]
  [:pre.invalid-line text])

(defn determine-class-level 
  [rendered-children level]
  ;; TODO probably shouldn't determine it like this?
  (if (some #(not= [:div.blank] %) 
            (rest rendered-children)) 
    (str "level-" level)
    "leaf-text"))

(defn page 
  [{:keys [title]} _]
  [:section.page
    [:div.page-title title]])

(defn heading 
  [{:keys [text]} leveltag]
  [:section
    [:div.heading-title {:class leveltag} text]])

(defn subheading 
  [{:keys [text]} leveltag]
  [:section
    [:div.subheading-title {:class leveltag} text]])

(defn divider
  [_ leveltag]
  [:hr {:class leveltag}])

(defn text-line
  [{:keys [text inline-comment]} leveltag]
  [:div.line-group
    [:div.line {:class leveltag}
      (when-not (s/blank? text) 
        [:div.text-line text])
      [:div.inline-comment inline-comment]]])

(defn list-item 
  [{:keys [text symbol inline-comment]} leveltag]
  [:div.list-group 
    [:div.list-row {:class leveltag}
      [:div.list-item
        [:div.bullet symbol]
        [:div.list-text text]]
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
    (create-hiccup structure -1))
  ([{:keys [type children plain-wrapper] :as structure} level]
    (if (sequential? structure)
      (map #(create-hiccup % level) structure)
      (let [render-fn (get render type)] 
        (if children
           (let [child-level       (inc level)
                 rendered-children (-> (into 
                                         (if plain-wrapper
                                           [:div]
                                           [:div.nested-group])
                                         (create-hiccup children child-level))
                                       (vec))
                 level-tag         (determine-class-level rendered-children child-level)]
             (if-not (empty? children)
               (conj (render-fn structure level-tag)
                   rendered-children)
               (render-fn structure level-tag)))
           (render-fn structure))))))
