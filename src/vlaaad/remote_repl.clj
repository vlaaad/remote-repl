(ns vlaaad.remote-repl
  (:import [java.net Socket]
           [java.io InputStreamReader BufferedReader OutputStreamWriter Reader]))

(defn repl
  "Start a socket repl that talks to a remote process

  Args:
  - :port (required) - target port
  - :host (optional) - target host"
  ([] (repl {}))
  ([k v & kvs] (repl (apply hash-map k v kvs)))
  ([{:keys [host port]}]
   {:pre [(some? port)]}
   (let [socket (Socket. ^String host ^int port)
         rd (-> socket .getInputStream InputStreamReader. BufferedReader.)
         wr (-> socket .getOutputStream OutputStreamWriter.)
         ^Reader in *in*]
     (doto (Thread. ^Runnable (fn []
                                (try
                                  (loop []
                                    (let [n (.read rd)]
                                      (when-not (neg? n)
                                        (print (char n))
                                        (flush)
                                        (recur))))
                                  (finally (.shutdownOutput socket)))))
       (.setName "vlaaad.remote-repl/repl")
       (.setDaemon true)
       (.start))
     (let [buf (char-array 1024)]
       (try
         (loop []
           (cond
             (.isOutputShutdown socket) nil
             (not (.ready in)) (do (Thread/sleep 50) (recur))
             :else (let [n (.read in buf)]
                     (when-not (neg? n)
                       (.write wr buf 0 n)
                       (.flush wr)
                       (recur)))))
         (finally (.close rd)))))))

(defn -main [& args]
  (apply repl (map (requiring-resolve 'clojure.edn/read-string) args)))