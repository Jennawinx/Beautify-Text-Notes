(ns beautify-text-notes.render
  (:require
    [clojure.string :as s]
    [hiccup.core :as h]
    [clojure.walk :as w :only [prewalk]]
    clojure.pprint))

(defn page 
  [_ rendered-children]
  [:div.page rendered-children])

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

(defn render-structure 
  [{:keys [type children] :as structure}]
  (if (sequential? structure)
    (-> (map render-structure structure)
        (into [:div.note-body])
        (vec))
    (let [render-fn (get render type)] 
       (when-not render-fn (print "nil" structure))
       (if children
          (render-fn structure (render-structure children))
          (render-fn structure)))))

; #_(let [counter     (atom 0)
;       print-touch (fn [x]
;                     (print (swap! counter inc) ":" (pr-str x) "→ "))
;       change-type (fn [x]
;                     (let [new-x (if (vector? x)
;                                   (apply list x)
;                                   (str x))]
;                       (prn new-x)
;                       new-x))]
;   (w/prewalk (fn [x]
;                           (print-touch x)
;                           (change-type x))
;                          [:a [:ba :bb] :c]))

