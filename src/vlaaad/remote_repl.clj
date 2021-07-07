(ns vlaaad.remote-repl
  (:import [java.net Socket SocketException InetAddress]
           [java.io InputStreamReader BufferedReader OutputStreamWriter Reader]))

(defn repl
  "Start a socket repl that talks to a remote process

  Args:
  - :port (required) - target port
  - :host (optional) - target host
  - :reconnect (optional, default false) - whether to keep automatically
    reconnecting to the repl on socket exceptions"
  ([] (repl {}))
  ([k v & kvs] (repl (apply hash-map k v kvs)))
  ([{:keys [host port reconnect]
     :or {reconnect false}
     :as opts}]
   {:pre [(some? port)
          (boolean? reconnect)]}
   (let [p (promise)
         _ (try
             (let [socket (Socket. ^String host ^int port)
                   rd (-> socket .getInputStream InputStreamReader. BufferedReader.)
                   wr (-> socket .getOutputStream OutputStreamWriter.)
                   ^Reader in *in*]
               (doto (Thread. ^Runnable (bound-fn []
                                          (try
                                            (loop []
                                              (let [n (.read rd)]
                                                (when-not (neg? n)
                                                  (print (char n))
                                                  (flush)
                                                  (recur))))
                                            (catch Exception e (p e))
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
                   (p nil)
                   (finally (.close rd)))))
             (catch Exception e
               (p e)))
         ret @p]
     (cond
       (and reconnect (instance? SocketException ret))
       (do (Thread/sleep 250)
           (println (str "Reconnecting to "
                         (or host (.getHostName (InetAddress/getByName nil)))
                         ":" port))
           (recur opts))

       (instance? Exception ret)
       (throw ret)

       :else
       ret))))

(defn -main [& args]
  (apply repl (map (requiring-resolve 'clojure.edn/read-string) args)))