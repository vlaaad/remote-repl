# Remote repl
[![Clojars Project](https://img.shields.io/clojars/v/vlaaad/remote-repl.svg)](https://clojars.org/vlaaad/remote-repl)

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
```
$ clj -Sdeps '{:deps {vlaaad/remote-repl {:mvn/version "1.2.12"}}}'
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
```
$ clj -Sdeps '{:deps {vlaaad/remote-repl {:mvn/version "1.2.12"}}}' -m vlaaad.remote-repl :port 5555
user=> (System/getProperty "clojure.server.repl")
"{:port 5555 :accept clojure.core.server/repl}"
user=> :repl/quit
```

You can use `-X` style invocation:
```
$ clj -Sdeps '{:deps {vlaaad/remote-repl {:mvn/version "1.2.12"}}}' -X vlaaad.remote-repl/repl :port 5555
```

You can install remote repl as a tool:
```
clj -Ttools install \
  vlaaad/remote-repl '{:git/url "https://github.com/vlaaad/remote-repl.git" :git/tag "v1.2.12"}' \
  :as remote-repl
clj -Tremote-repl repl :port 5555
```

## Reconnecting

It might be useful to automatically reconnect to the remote REPL. For 
example, you might want to restart your REPL server during development to update
dependencies. You can use `:reconnect true` option to keep the REPL client 
reconnecting, that way it will keep connecting to the remote REPL while the JVM is alive.

Example:

```shell
$ clj -Sdeps '{:deps {vlaaad/remote-repl {:mvn/version "1.2.12"}}}' \
  -X vlaaad.remote-repl/repl \
  :port 5757 :reconnect true
Reconnecting to localhost:5757
Reconnecting to localhost:5757
...
```

Then, start a REPL server on port 5757 in a different terminal:

```shell
$ clj -J-Dclojure.server.repl='{:port 5555 :accept clojure.core.server/repl}'
```

Once it starts, first terminal will establish the connection:
```shell
...
Reconnecting to localhost:5757
Reconnecting to localhost:5757
user=> 
```

## Acknowledgements

This project is similar to [tubular](https://github.com/mfikes/tubular), but 
smaller (only 70 lines of code and no dependencies). It is inspired by 
[clojure.core.server/remote-prepl](https://github.com/clojure/clojure/blob/0035cd8d73517e7475cb8b96c7911eb0c43a1a9d/src/clj/clojure/core/server.clj#L295-L338),
but does not require its target to be a prepl.