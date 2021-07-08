(ns build
  (:require [hf.depstar.uberjar :as jar]
            [clojure.tools.deps.alpha :as deps]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.data.xml :as xml]
            [clojure.string :as str]
            [cemerick.pomegranate.aether :as aether]))

(xml/alias-uri 'pom "http://maven.apache.org/POM/4.0.0")
(xml/alias-uri 'xsi "http://www.w3.org/2001/XMLSchema-instance")

(defn- sh! [& args]
  (let [{:keys [exit out] :as ret} (apply sh/sh args)]
    (if (zero? exit)
      (str/trim-newline out)
      (throw (ex-info "Command failed" (assoc ret :args args))))))

(defmacro with-progress [label-expr & body]
  `(do
     (println (str ~label-expr "..."))
     (let [ret# (do ~@body)]
       (println "Done.")
       ret#)))

(defn get-version [opts]
  (assoc opts :version (str "1.2." (sh! "git" "rev-list" "HEAD" "--count"))))

(defn tag [{:keys [version] :as opts}]
  (with-progress "Tagging"
    (let [tag (str "v" version)]
      (sh! "git" "tag" tag)
      (sh! "git" "push" "origin" tag)
      (assoc opts :tag tag))))

(defn get-basis [opts]
  (let [{:keys [root-edn user-edn project-edn]} (deps/find-edn-maps "deps.edn")]
    (assoc opts :basis (deps/calc-basis (deps/merge-edns [root-edn user-edn project-edn])))))

(defn pom [{:keys [version basis] :as opts}]
  (with-progress "Creating pom.xml"
    (let [pom-file "pom.xml"]
      (with-open [writer (io/writer pom-file)]
        (xml/indent
          (xml/sexp-as-element
            [::pom/project
             {:xmlns "http://maven.apache.org/POM/4.0.0"
              :xmlns/xsi "http://www.w3.org/2001/XMLSchema-instance"
              ::xsi/schemaLocation "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"}
             [::pom/modelVersion "4.0.0"]
             [::pom/packaging "jar"]
             [::pom/groupId "vlaaad"]
             [::pom/artifactId "remote-repl"]
             [::pom/version version]
             [::pom/name "remote-repl"]
             [::pom/dependencies
              (for [[sym coord] (:deps basis)
                    :when (:mvn/version coord)]
                [::pom/dependency
                 [::pom/groupId (namespace sym)]
                 [::pom/artifactId (name sym)]
                 [::pom/version (:mvn/version coord)]])]
             [::pom/build
              (for [dir (:paths basis)]
                [::pom/sourceDirectory dir])]
             [::pom/repositories
              (for [[id {:keys [url]}] (:mvn/repos basis)
                    :when (not= url "https://repo1.maven.org/maven2/")]
                [::pom/repository
                 [::pom/id id]
                 [::pom/url url]])]
             [::pom/scm
              [::pom/url "https://github.com/vlaaad/remote-repl"]
              [::pom/tag (sh! "git" "rev-parse" "HEAD")]]])
          writer))
      (assoc opts :pom-file pom-file))))

(defn jar [{:keys [version] :as opts}]
  (with-progress "Creating jar"
    (let [jar-file (str version ".jar")]
      (jar/build-jar {:jar jar-file :jar-type :thin})
      (assoc opts :jar-file jar-file))))

(defn deploy [{:keys [version basis pom-file jar-file] :as opts}]
  (with-progress "Deploying"
    (let [ret (aether/deploy
                :coordinates ['vlaaad/remote-repl version]
                :pom-file pom-file
                :jar-file jar-file
                :repository (-> basis
                                :mvn/repos
                                (select-keys ["clojars"])
                                (update "clojars" assoc
                                        :username "vlaaad"
                                        :password (-> (System/console)
                                                      (.readPassword "Clojars token: " (object-array 0))
                                                      String/valueOf))))]
      (assoc opts :deploy-result ret))))

(defn clean [{:keys [pom-file jar-file] :as opts}]
  (.delete (io/file pom-file))
  (.delete (io/file jar-file))
  (dissoc opts :pom-file :jar-file))

(defn release [opts]
  (-> opts
      get-basis
      get-version
      tag
      pom
      jar
      deploy
      clean))