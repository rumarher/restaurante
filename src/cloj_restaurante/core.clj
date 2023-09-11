(ns cloj-restaurante.core
  (:gen-class)
  (:require
   [clojure.core.async
    :as a
    :refer [>! <! go go-loop]]
   [cloj-restaurante.datafood :refer [save-order food-with-prices get-status-order]]
   [cloj-restaurante.userside :refer [incoming-orders]]
   [clojure.data.json :refer [write-str]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults ]]
   [ring.middleware.json :refer [wrap-json-params wrap-json-body]]
   [ring.util.response :refer [response content-type]]
   [ring.adapter.jetty :as jetty]
   [compojure.core :refer [defroutes GET POST]]
   [compojure.route :as route]))

(def bye (atom false))

(defn to-exit []
  (reset! bye true))

(defn save-order-async
  []
  (go-loop []
    (println "Working...")
    (Thread/sleep (+ (rand 7000) 1000)) ;; wait for 1s - 10s
    (let [order-received (<! incoming-orders)
          uuid-order (save-order order-received)]
      (println "Order: " order-received " received!, this is your order: " uuid-order))
    (when @bye
      (recur))))

(defn store-order-async
  [the-order]
  (go []
      (>! incoming-orders the-order)))

(defroutes app-routes
  (GET "/" [] "<h1>Hello World!</h1>")
  (GET "/menu" [] (content-type (response(write-str @food-with-prices)) "application/json"))
  (GET "/orders" request
    (let [uuid-order (get (:body request) "uuid-order")]
      (println uuid-order ": " (get-status-order uuid-order))
      "eee"))
  
  (POST "/to-order" request
    ;; (store-order-async (order-json-to-multiple-food-order request))
    (println request (class request))
    (let [order (:body request)]
      (store-order-async order)
      (println (class order) order))
    "response")
  (route/not-found "<h1>Page not found.</h1>"))

(def handler-app
  (wrap-json-body
   (wrap-defaults
    app-routes (assoc-in site-defaults [:security :anti-forgery] false))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "######Comienza el servidor en main########")
  (save-order-async)
  (println "######Escuchamos peticiones en main########")
  (jetty/run-jetty handler-app {:port 3000 :join? true}))
