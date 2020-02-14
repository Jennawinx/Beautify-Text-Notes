(ns beautify-text-notes.core
  (:require
    [clojure.string :as s]
    [hiccup.core :as h]
    [beautify-text-notes.parser :as p]
    clojure.pprint)
  (:gen-class))

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
     #_(-> (map p/mdc->structure file-list)
         ;; ^ is a lazy seq
         (prn))

     (clojure.pprint/pprint (p/mdc->structure "./resources/test1.mdc"))
     ; (clojure.pprint/pprint (mdc->structure "./resources/react.mdc"))
          

     #_(prn "        - renders components on client and server           <- reseach this")
     #_(-> (parse-line 1 "        - renders components on client and server           <- reseach this")
         (prn))

     ;; Write output
     ))



