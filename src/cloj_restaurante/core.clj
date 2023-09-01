(ns cloj-restaurante.core
  (:gen-class)
  (:require
   [clojure.core.async
    :as a
    :refer [>! <! go go-loop]]
   [cloj-restaurante.datafood :refer [save-order food food-with-prices]]
   [cloj-restaurante.userside :refer [incoming-orders]]
   [clojure.data.json :refer [write-str read-str]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults ]]
   [ring.util.response :refer [response content-type]]
   [ring.adapter.jetty :as jetty]
   [compojure.core :refer [defroutes GET POST]]
   [compojure.route :as route]))

(defn save-order-async
  []
  (go-loop []
    (println "Working...")
    (Thread/sleep (+ (rand 7000) 1000)) ;; wait for 1s - 10s
    (let [order-received (<! incoming-orders)]
      (save-order order-received)
      (println "Order received!: " order-received)
      (recur))))

(defn store-order-async
  [the-order]
  (go []
      (>! incoming-orders the-order)))

(defn order-json-to-multiple-food-order
  [the-order]
  (flatten (reduce #(conj %1 (repeat (val %2) (key %2))) '() the-order)))

(defroutes app-routes
  (GET "/" [] "<h1>Hello World!</h1>")
  (GET "/menu" [] (content-type (response(write-str @food-with-prices)) "application/json"))
  (POST "/to-order" request
    (let [new-order (read-str (slurp (:body request)))]
      (println new-order)
      (store-order-async (order-json-to-multiple-food-order new-order))
      (write-str new-order)))
  (route/not-found "<h1>Page not found.</h1>"))

(def handler-app
  (wrap-defaults app-routes (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "######Comienza el servidor en main########")
  (save-order-async)
  (println "######Escuchamos peticiones en main########")
  (jetty/run-jetty handler-app {:port 3000 :join? true}))
