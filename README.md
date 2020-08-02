# Remote repl

## Rationale

Sometimes I want to open a remote socket repl from my repl, but Clojure does not 
provide a way to do this out of the box. 

## Usage

To give it a try, you need a socket repl available on the network. You can start
it with this shell command:
```sh
$ clj "-J-Dclojure.server.repl={:port 5555 :accept clojure.core.server/repl}" 
```

Now you can connect to that process from another process
```sh
$ clj
Clojure 1.10.1
user=> (require '[vlaaad.remote-repl :as rr])
nil
user=> (rr/repl :port 5555)
;; at this point, forms sent to the repl are evaluated on the remote process 
user=> (System/getProperty "clojure.server.repl")
"{:port 5555 :accept clojure.core.server/repl}"
user=> :repl/quit
;; now we are back to evaluating in our local process.
nil
user=> 
```

You can use `-main` to immediately drop into a remote repl:
```sh
$ clj -m vlaaad.remote-repl :port 5555
user=> (System/getProperty "clojure.server.repl")
"{:port 5555 :accept clojure.core.server/repl}"
user=> :repl/quit
```

## Acknowledgements

This project is similar to [tubular](https://github.com/mfikes/tubular), but 
smaller (only 50 lines of code and no dependencies). It is inspired by 
[clojure.core.server/remote-prepl](https://github.com/clojure/clojure/blob/0035cd8d73517e7475cb8b96c7911eb0c43a1a9d/src/clj/clojure/core/server.clj#L295-L338),
but does not require its target to be a prepl.