(ns beautify-text-notes.core
  (:require
    [clojure.string :as s]
    [beautify-text-notes.parser :as p]
    [beautify-text-notes.render :as r]
    [selmer.parser :as sp]
    [beautify-web.core :as bw]
    [hiccup.core :as h]
    clojure.pprint)
  (:gen-class))

"
File I/O 
https://www.tutorialspoint.com/clojure/clojure_file_io.htm

TODO make parser work with tabs
"

(def default-settings 
  {"root-dir" "./", 
   "save-dir" "./",
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

(defn render-html
  [rendered-pages notebook-name]
  (sp/render-file "content.html" 
    {:notebook-name notebook-name
     :css           (slurp (clojure.java.io/resource "content.css"))
     :time-stamp    (.toString (java.util.Date.))
     :pages         rendered-pages}))

(defn prt [x msg] (prn msg) x)

(defn -main
  ""
  []
  (let [;; Read settings
        settings  (get-settings)
        {:strs [root-dir save-dir save-as notebook-name]} settings
        save-file (str save-dir save-as)
        file-list (mapv #(str root-dir %) 
                        (filter #(s/ends-with? % ".mdc") 
                        (seq (.list (clojure.java.io/file root-dir)))))]
     
     (prn)
     (prn (get-settings))
     ; (prn root-dir)
     (prn file-list)
     (prn)
     (prn)

     (-> ;; Read file in list, parse each one, render a html per page
         (map #(-> (p/mdc->structure %)
                   (prt "mdc->structure complete")
                   (r/create-hiccup)
                   (prt "structure->hiccup complete")
                   (h/html)
                   (prt "hiccup->html complete")
                   ;; this can break if there are weird characters
                   (bw/beautify-html)
                   (prt "html->pretty-html complete"))
              file-list)
         ;; Add pages to html template and save
         (render-html notebook-name)
         (prt "pages inserted into html template complete")
         (create-html save-file)
         (prt "html file generation complete"))
     ))



