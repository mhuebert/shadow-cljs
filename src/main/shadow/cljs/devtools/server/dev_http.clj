(ns shadow.cljs.devtools.server.dev-http
  "provides a basic static http server per build"
  (:require [clojure.java.io :as io]
            [shadow.cljs.devtools.server.web.common :as common]
            [ring.middleware.resource :as ring-resource]
            [ring.middleware.file :as ring-file]
            [ring.middleware.file-info :as ring-file-info]
            [ring.middleware.content-type :as ring-content-type]
            [aleph.http :as aleph]
            [aleph.netty :as netty]
            [clojure.tools.logging :as log]
            [shadow.cljs.devtools.config :as config]))

(defn disable-all-kinds-of-caching [handler]
  ;; this is strictly a dev server and caching is not wanted for anything
  ;; basically emulates having devtools open with "Disable cache" active
  (fn [req]
    (-> req
        (handler)
        (update-in [:headers] assoc
          "cache-control" "max-age=0, no-cache, no-store, must-revalidate"
          "pragma" "no-cache"
          "expires" "0"))))

(defn start-build-server
  [executor {:keys [build-id http-root http-port]
             :or {http-port 0}}]

  (let [root-dir (io/file http-root)]
    (when-not (.exists root-dir)
      (io/make-parents (io/file root-dir "index.html")))

    (let [http-handler
          (-> common/not-found
              ;; some default resources, only used if no file exists
              ;; currently only contains the CLJS logo as favicon.ico
              ;; pretty much only doing this because of the annoying
              ;; 404 chrome devtools show then no icon exists
              (ring-resource/wrap-resource "shadow/cljs/devtools/server/dev_http")
              (ring-content-type/wrap-content-type)

              (ring-file/wrap-file root-dir {:allow-symlinks? true
                                             :index-files? true})
              (ring-file-info/wrap-file-info
                ;; source maps
                {"map" "application/json"})

              (disable-all-kinds-of-caching))

          instance
          (aleph/start-server http-handler
            {:port http-port
             :executor executor
             :shutdown-executor? false})

          port
          (netty/port instance)]

      ;; FIXME: this should show a proper message somewhere
      (log/info ::http-serve {:http-port port :http-root http-root :build-id build-id})

      instance)))

(def http-keys
  [:http-port
   :http-root])

(defn get-server-configs []
  (let [{:keys [builds] :as config}
        (config/load-cljs-edn!)]

    (->> (vals builds)
         (map (fn [{:keys [build-id devtools] :as build-config}]
                (let [http-config (select-keys devtools http-keys)]
                  (when-not (empty? http-config)
                    (assoc http-config :build-id build-id)))))
         (remove nil?)
         (into []))))

(comment
  (get-server-configs))

;; FIXME: use config watch to restart servers on config change
(defn start [executor]
  (let [configs
        (get-server-configs)

        servers
        (into [] (map #(start-build-server executor %)) configs)]

    {:servers servers
     :configs configs}
    ))

(defn stop [{:keys [servers] :as svc}]
  (doseq [srv servers]
    (.close srv)))