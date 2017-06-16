(ns shadow.cljs.npm.client
  (:require [cljs.reader :as reader]
            ["readline" :as rl]
            ["net" :as node-net]
            [shadow.cljs.npm.util :as util]
            [clojure.string :as str]))

(defn run [project-root config server-pid args]
  (let [{:keys [socket-repl] :as ports}
        (-> (util/slurp server-pid)
            (reader/read-string))]

    (if-not socket-repl
      (prn [:no-socket-repl-port server-pid ports])

      (let [socket
            (node-net/connect socket-repl "localhost")

            last-prompt-ref
            (volatile! nil)

            rl
            (rl/createInterface
              #js {:input js/process.stdin
                   :output js/process.stdout
                   :completer
                   (fn [prefix callback]
                     (let [last-prompt @last-prompt-ref]
                       ;; without a prompt we can't autocomplete
                       (if-not last-prompt
                         (callback nil (clj->js [[] prefix]))

                         ;; FIXME: hook this up properly
                         (callback nil (clj->js [[] prefix])))))})

            write
            (fn [text]
              ;; assume that everything we send is (read) which reads something
              ;; we can never autocomplete
              ;; and only a new prompt enables it
              (vreset! last-prompt-ref nil)
              (.write socket text))]

        (.on socket "connect"
          (fn [err]
            (if err
              (println "shadow-cljs - socket connect failed")

              (do (println "shadow-cljs - connected to server")

                  ;; FIXME: this is an ugly hack that will be removed soon
                  (when-not (= ["--repl"] args)
                    (write (str "(shadow.cljs.devtools.cli/from-remote " (pr-str (into [] args)) ")\n"))
                    (when-not (some #{"--dev"} args)
                      (write (str ":repl/quit\n"))))

                  (.on rl "line"
                    (fn [line]
                      (write (str line "\n"))))

                  ;; CTRL+D closes the rl
                  (.on rl "close"
                    (fn []
                      (.end socket)
                      (println)))
                  ))))

        (.on socket "data"
          (fn [data]
            (.pause rl)
            (let [txt (.toString data)

                  prompts
                  (re-seq #"\[(\d+):(\d+)\]\~([^=> \n]+)=> " txt)]

              (doseq [[prompt root-id level-id ns :as m] prompts]
                (vreset! last-prompt-ref {:text prompt
                                          :ns (symbol ns)
                                          :level (js/parseInt level-id 10)
                                          :root (js/parseInt root-id 10)})
                (.setPrompt rl prompt))

              (js/process.stdout.write txt)
              (.resume rl)
              )))

        (.on socket "end" #(.close rl))
        ))))
