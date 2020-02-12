(ns beautify-text-notes.core
  (:require
    [clojure.string :as s]
    [hiccup.core :as h]
    clojure.pprint)
  (:gen-class))

;; TODO rename to prettify-text-notes

(def list-symbol-regex #"^[-*o](?=\s.+)|\d+\.(?=\s.+)")
(def heading-title-regex #"(?!=)(?<=====).+(?<!=)(?=====)")
(def subheading-title-regex #"(?!-)(?<=----).+(?<!-)(?=----)")

"
File I/O 
https://www.tutorialspoint.com/clojure/clojure_file_io.htm
"

(def default-settings 
  {"root-dir" ".", 
   "save-dir" ".",
   "save-as" "output.html",
   "color-level1" "maroon", 
   "color-level2" "salmon",
   "color-level3" "lightblue"})

(defn create-html 
  [save-as hiccup]
  (with-open [w (clojure.java.io/writer save-as :append false)]
    (.write w (h/html hiccup))))

(defn consume-indent 
  [line] ;; -> num-spaces rest-line
  (let [trimed (s/triml line)]
    [(- (.length line) (.length trimed))
     trimed])) 

(defn get-inline-comment 
  [columns]
  (let [text (last columns)] 
    (if (and text
             (or (s/starts-with? text "// ")
                 (s/starts-with? text "# ")
                 (s/starts-with? text "<- ")))
      text
      "")))

(defn list-item?
  [text]
  (some? (re-find list-symbol-regex text)))

(defn list-item 
  [level text inline-comment]
  (let [symbol (re-find list-symbol-regex text)] 
    {:type           :list
     :symbol         symbol
     :text           (subs text (.length symbol))
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

(defn invalid-line 
  [level line & [error]]
  {:type      :invalid-line
   :text      line
   :level     level
   :error-msg error})

(defn blank
  []
  {:type :blank
   :level 0})

(defn parse-line 
  ""
  [last-level line]
  ;; Using regex to parse because efficiency is not a concern
  (let [[num-spaces rest-line]       (consume-indent line)
        level                        (/ num-spaces 4)
        columns                      (s/split rest-line #"\s\s\s\s+")
        inline-comment               (get-inline-comment columns)
        columns                      (if-not (s/blank? inline-comment) (pop columns) columns)
        num-columns                  (.length columns)]
      
    ; (prn num-spaces rest-line)
    ; (prn level)
    ; (prn inline-comment)

    ; (prn (list-item? columns))
    (prn "columns" columns num-columns)

    (cond 
      ;; check for blank lines
      (s/blank? line)
      (blank)

      ;; check of invalid indentation/structures
      (not (and (int? level)
                (<= level (inc last-level))))
      (invalid-line level line "invalid indentation")
      
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
             (do
               ; (prn)
               ; (prn "            plain type" type)
               ; (prn)
               (update-in data (conj (get level-pointers (dec last-level)) :children) conj parsed-line))
             (let [parent-location     (get level-pointers (dec level))

                   ; _ (prn)
                   ; _ (prn "            type" type)
                   ; _ (prn "            level" level)
                   ; _ (prn "            text" text)
                   ; _ (prn "            ploc" parent-location)
                   ; _ (prn "            loc" (conj parent-location :children))
                   ; _ (prn "            pointers" level-pointers)
                   ; _ (prn "            get is nil?" (nil? (get-in data (conj parent-location :children))))
                   ; _ (prn)

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

#_(create-html 
       save-file
       [:html 
         [:head [:title "Hello"]]
         [:body [:div {:style {:color "red"}} [:p "Hello World!"]]]])

(defn get-settings 
  []
  ;; TODO need try-catch
  ; (println (.exists (clojure.java.io/file "./settings.mdcs")))
  (if (.exists (clojure.java.io/file "./settings.mdcs"))
    (with-open [rdr (clojure.java.io/reader "./settings.mdcs")]
       (reduce (fn [result line]
           (->> (s/split line #":\s+")
                (apply assoc result)))
           {}
           (line-seq rdr)))
    default-settings))

(defn -main
  ""
  []
  (let [;; Read settings
        settings  (get-settings)
        {:strs [root-dir save-dir save-as]} settings
        save-file (str save-dir save-as)
        file-list (mapv str (filter #(and (.isFile %) (s/ends-with? % ".mdc")) 
                                    (file-seq (clojure.java.io/file root-dir))))]
     
     ; (prn (get-settings))
     ; (prn root-dir)
     ; (prn file-list)
     

     ;; Read file in list and parse each one
     #_(-> (map mdc->structure file-list)
         ;; ^ is a lazy seq
         (prn))

     (clojure.pprint/pprint (mdc->structure "./resources/test1.mdc"))
     ; (clojure.pprint/pprint (mdc->structure "./resources/react.mdc"))
          

     #_(prn "        - renders components on client and server           <- reseach this")
     #_(-> (parse-line 1 "        - renders components on client and server           <- reseach this")
         (prn))

     ;; Write output
     ))



