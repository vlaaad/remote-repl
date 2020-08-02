(ns deploy
  (:require [cemerick.pomegranate.aether :as aether]))

(defn -main [version]
  (aether/deploy
    :coordinates ['vlaaad/remote-repl version]
    :jar-file "remote-repl.jar"
    :pom-file "pom.xml"
    :repository {"clojars" {:url "https://clojars.org/repo"
                            :username "vlaaad"
                            :password (-> (System/console)
                                          (.readPassword "Deploy token: " (object-array 0))
                                          (String.))}}))