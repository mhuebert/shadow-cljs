(ns demo.selfhost
  (:require [cljs.js :as cljs]
            [clojure.string] ;; reagent doesn't declare this dep
            [shadow.bootstrap :as boot]
            [cljs.env :as env]))

(defn print-result [{:keys [error value] :as result}]
  (js/console.log "result" result)
  (js/console.log "compile-state" @boot/compile-state-ref)
  (set! (.-innerHTML (js/document.getElementById "dump")) value))

(def code
  "
(ns simpleexample.core
  (:require [clojure.string]
            [reagent.core :as r]))

(defonce timer (r/atom (js/Date.)))

(defonce time-color (r/atom \"#f34\"))

(defonce time-updater (js/setInterval
                       #(reset! timer (js/Date.)) 1000))

(defn greeting [message]
  [:h1 message])

(defn clock []
  (let [time-str (-> @timer .toTimeString (clojure.string/split \" \") first)]
    [:div.example-clock
     {:style {:color @time-color}}
     time-str]))

(defn color-input []
  [:div.color-input
   \"Time color: \"
   [:input {:type \"text\"
            :value @time-color
            :on-change #(reset! time-color (-> % .-target .-value))}]])

(defn simple-example []
  [:div
   [greeting \"Hello world, it is now\"]
   [clock]
   [color-input]])

(r/render [simple-example] (js/document.getElementById \"app\"))")

(defn compile-it []
  (cljs/eval-str
    boot/compile-state-ref
    code
    "[test]"
    {:eval cljs/js-eval
     :analyze-deps false
     :verbose true
     :load boot/load}
    print-result))

(defn start []
  (boot/init compile-it))

(defn stop [])
