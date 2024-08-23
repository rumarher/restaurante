(ns cloj-restaurante.core
  (:gen-class)
  (:require
   [clojure.core.async
    :as a
    :refer [>! <! go go-loop]]
   [cloj-restaurante.datafood :refer [save-order
                                      food-with-prices
                                      get-order-status
                                      get-all-orders]]
   [cloj-restaurante.userside :refer [incoming-orders
                                      start-cooking-order
                                      start-deliver
                                      to-refuse
                                      put-in-pending
                                      finalize-order
				      repl-options]]
   [clojure.main :refer [repl]]
   [clojure.data.json :refer [write-str]]
   [ring.middleware.defaults :refer [wrap-defaults
                                     site-defaults ]]
   [ring.middleware.json :refer [wrap-json-body]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.util.response :refer [response
                               content-type]]
   [ring.adapter.jetty :as jetty]
   [compojure.core :refer [defroutes
                           GET
                           POST
                           PATCH]]
   [compojure.route :as route]))

(def bye (atom false))

(defn to-exit []
  (reset! bye true))

(defn save-order-callb
  []
  (go-loop []
    (println "Working...")
    ;;(Thread/sleep (+ (rand 7000) 1000)) ;; wait for 1s - 10s
    (let [order-received (<! incoming-orders)
          uuid-order (save-order order-received)]
      (println "Order: " order-received " received!, this is your order: " uuid-order))
    (when (not @bye)
      (recur))))

(defn store-order-async
  [the-order]
  (go []
    (>! incoming-orders the-order)))

(defn change-order-status
  [the-order new-status]
  (case new-status
    "cooking" (start-cooking-order the-order)
    "in delivery" (start-deliver the-order)
    "refused" (to-refuse the-order)
    "pending" (put-in-pending the-order)
    "done" (finalize-order the-order)
    (println "Orden: " new-status "incorrecta")))

(defroutes app-routes
  (GET "/" [] "<h1>Hello World!</h1>")
  (GET "/menu" [] (content-type (response(write-str @food-with-prices)) "application/json"))
  (GET "/order-status" request
       (let [uuid-order (get (:body request) "uuid-order")]
         (println uuid-order ": " (get-order-status uuid-order))))
  (PATCH "/change-order-status" request (let [uuid-order  (get (:body request) "uuid-order")
                                              next-status (get (:body request) "next-status")]
                                          (change-order-status uuid-order next-status)))
  (GET "/all-orders" [] (content-type (response(write-str (get-all-orders))) "application/json"))
  (POST "/to-order" request
        ;; (store-order-async (order-json-to-multiple-food-order request))
        (println request (class request))
        (let [order (:body request)]
          (store-order-async order)
          (println (class order) order))
        "response")
  (route/not-found "<h1>Page not found.</h1>"))

(def handler-app
  (-> app-routes 
      (wrap-json-body)
       (wrap-cors
	:access-control-allow-methods [:get :put :post :delete :options]
	;; aquí va la dirección de donde se esté ejecutando el front-end
	:access-control-allow-origin [#"http://localhost:3001"]
	:access-control-allow-headers ["Content-Type" "Authorization"])
       (wrap-defaults
	(assoc-in site-defaults [:security :anti-forgery] false))))


#_
;; Antigua Configuración
(def handler-app
  (wrap-json-body
   (wrap-cors
    (wrap-defaults
     app-routes 
     (assoc-in site-defaults [:security :anti-forgery] false))
    :access-control-allow-methods [:get :put :post :delete :options]
    ;; aquí va la dirección de donde se esté ejecutando el front-end
    :access-control-allow-origin [#"http://localhost:3001"]
    :access-control-allow-headers ["Content-Type" "Authorization"])))


;; (apply clojure.main/repl repl-options)

(defn -interactive-init
  []
  (apply clojure.main/repl repl-options))

(defn -server-init
  []
  (println "######Comienza el servidor en main########")
  (save-order-callb)
  (println "######Escuchamos peticiones en main########")
  (jetty/run-jetty handler-app {:port 3000 :join? true}))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (case (first args)
    "-server" (-server-init)
    "-interactive" (-interactive-init)
    (println "NO me has dicho nada... -server o -interactive")))


