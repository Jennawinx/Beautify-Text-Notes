(ns beautify-text-notes.core
  (:require
    [clojure.string :as s]
    [beautify-text-notes.parser :as p]
    [beautify-text-notes.render :as r]
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
   "color-level3" "lightblue"
   "notebook-name" "My Notebook"})

(defn create-html 
  [html save-as]
  (with-open [w (clojure.java.io/writer save-as :append false)]
    (.write w html)))

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
        {:strs [root-dir save-dir save-as notebook-name]} settings
        save-file (str save-dir save-as)
        file-list (mapv str (filter #(and (.isFile %) (s/ends-with? % ".mdc")) 
                                    (file-seq (clojure.java.io/file root-dir))))]
     
     ; (prn (get-settings))
     ; (prn root-dir)
     ; (prn file-list)
     
     ;; Read file in list, parse each one, render and save to html
     #_(-> (map p/mdc->structure file-list)
         ;; ^ is a lazy seq
         (prn))

     (-> (p/mdc->structure "./test-resources/test1.mdc")
         (r/render-structure notebook-name)
         (create-html save-file)
         ; (clojure.pprint/pprint)
         )     
     ))



