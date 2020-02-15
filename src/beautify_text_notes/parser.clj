(ns beautify-text-notes.parser
  (:require
    [clojure.string :as s]
    clojure.pprint))

;; TODO, probably don't need to pass level all the way, level can be inferred

;; TODO rename to prettify-text-notes

(def heading-title-regex #"(?!=)(?<=====).+(?<!=)(?=====)")
(def subheading-title-regex #"(?!-)(?<=----).+(?<!-)(?=----)")
(def check-list-regex #"^([-*o•]|\d+\.)\s+.+")
(def parse-list-regex #"^([-*o•]|\d+\.)\s+(.+)")
(def parse-inline-comment-regex #"^(\/\/|#|<-)\s+(.+)")
(def divider-regex #"^----+$")

(defn consume-indent 
  [line] ;; -> num-spaces rest-line
  (let [trimed (s/triml line)]
    [(- (.length line) (.length trimed))
     trimed])) 

(defn get-inline-comment 
  [comment]
  (if-let [[_ symbol comment] (some->> comment (re-find parse-inline-comment-regex))]
    comment
    ""))

(defn list-item?
  [text]
  (some? (re-find check-list-regex text)))

(defn list-item 
  [level text inline-comment]
  (let [[_ symbol text] (re-find parse-list-regex text)] 
    {:type           :list
     :symbol         symbol
     :text           text
     :inline-comment inline-comment
     :level          level
     :children       []}))

(defn text-line
  [level text inline-comment]
  {:type           :line
   :inline-comment inline-comment
   :text           text
   :level          level
   :children       []})

(defn divider
  [level text]
  {:type :blank
   :level level})

(defn heading?
  [text]
  (some? (re-find heading-title-regex text)))

(defn heading 
  [level text]
  {:type     :heading
   :text     (re-find heading-title-regex text)
   :level    level
   :children []})

(defn subheading?
  [text]
  (some? (re-find subheading-title-regex text)))

(defn subheading 
  [level text]
  {:type     :subheading
   :text     (re-find subheading-title-regex text)
   :level    level
   :children []})

(defn divider?
  [text]
  (some? (re-find divider-regex text)))

(defn divider
  [level]
  {:type     :divider
   :level    level
   :children []})

(defn invalid-line 
  [level line & [error]]
  {:type      :invalid-line
   :text      line
   :level     level
   :error-msg error})

(defn blank
  [level]
  {:type :blank
   :level level})

(defn parse-line 
  ""
  [last-level line]
  ;; Using regex to parse because efficiency is not a concern
  (let [[num-spaces rest-line]       (consume-indent line)
        level                        (/ num-spaces 4)
        columns                      (s/split rest-line #"\s\s\s\s+")
        inline-comment               (get-inline-comment (last columns))
        columns                      (if-not (s/blank? inline-comment) (pop columns) columns)
        num-columns                  (.length columns)]
      
    ; (prn num-spaces rest-line)
    ; (prn level)
    ; (prn inline-comment)

    ; (prn (list-item? columns))
    ; (prn "columns" columns num-columns)

    (cond 
      ;; check for blank lines
      (s/blank? line)
      (blank last-level)

      ;; check of invalid indentation/structures
      (not (and (int? level)
                (<= level (inc last-level))))
      (invalid-line level line "invalid indentation")

      ;; single-line comment
      (and (= num-columns 0) inline-comment)
      (text-line level "" inline-comment)

      ;; single columns
      (= num-columns 1)
      (let [text (first columns)]
        (cond 
          ;; check for list items
          (list-item? text)
          (list-item level text inline-comment)

          ;; check for heading 
          (heading? text)
          (heading level text)

          ;; check for subheading
          (subheading? text)
          (subheading level text)
          
          ;; check for divider
          (divider? text)
          (divider level)

          ;; check for single lines
          :else
          (text-line level text inline-comment)))

      ;; rest are invalid
      :else 
      (invalid-line level line))))

(defn parse-mdc
  [{:keys [last-line level-pointers last-level result] :as data} line]
         ; (prn last)
         (prn line)
         (let [{:keys [level text type] :as parsed-line} (parse-line last-level line)
               data   (assoc data :last-line parsed-line)]

           (if (contains? #{:blank :invalid-line} type)
             (update-in data (conj (get level-pointers last-level) :children) conj parsed-line)
             (let [parent-location     (get level-pointers (dec level))

                   _ (prn)
                   _ (prn "            type" type)
                   ; _ (prn "            level" level)
                   ; _ (prn "            text" text)
                   ; _ (prn "            ploc" parent-location)
                   _ (prn "            loc" (conj parent-location :children))
                   ; _ (prn "            pointers" level-pointers)
                   ; _ (prn "            get is nil?" (nil? (get-in data (conj parent-location :children))))
                   _ (prn)

                   num-parent-children (.length (get-in data (conj parent-location :children)))
                   insert-to           (conj parent-location :children num-parent-children)]

               (-> data
                 (assoc :last-line parsed-line)
                 (assoc :last-level level)
                 (assoc-in [:level-pointers level] insert-to)
                 (assoc-in insert-to parsed-line))))
           ))

(defn mdc->structure
  ^{:return :structure}
  [file]
  (prn "####################" file "####################")
  (with-open [rdr (clojure.java.io/reader file)]
     (reduce parse-mdc
       {:level-pointers {-1    [:result] 
                         0     [:result]}
        :last-line      nil
        :last-level     0
        :result         {:type :page
                         :children []}}
     (line-seq rdr))))