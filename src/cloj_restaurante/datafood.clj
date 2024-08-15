(ns cloj-restaurante.datafood
  (:gen-class)
  (:require [clojure.java.jdbc :as j]
            [clojure.data.json :refer [write-str]]
            [clojure.set :refer [subset?]]))

(def h2-db {:dbtype "h2"
            :user "sa"
            :dbname "./resources/myrest"
            :classname "org.h2.Driver"
            :port "8082"})

(j/db-do-commands h2-db
                  [(j/drop-table-ddl :orders {:conditional? true})
                   (j/drop-table-ddl :menu {:conditional? true})
		   
                   (j/create-table-ddl :menu
                                       [[:id :bigint :auto_increment :primary :key]
                                        [:name "varchar(32)" "not null"]
                                        [:price :int "not null"]] {:conditional? true})
                   (j/create-table-ddl :orders
                                       [[:id :bigint :auto_increment]
                                        [:the_order :json "not null"]
                                        [:status "varchar(32)" "not null"]
                                        [:date_received :date "not null"]
                                        [:short_token "varchar(4)" "not null"]
                                        [:deadline :date]
                                        [:random_token :uuid :primary :key "not null"]] {:conditional? true})])

(j/insert-multi! h2-db :menu
                 [{:name "arroz" :price 50}
                  {:name "tortilla" :price 100}
                  {:name "refresco" :price 60}
                  {:name "lomo" :price 90}
                  {:name "bienmesabe" :price 80}])


(defn encode-human-readable-base32
  [value]
  (let [acum-string ""
        chars-b32 "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"]
    (loop [x value
           u acum-string]
      (let [q (quot x 32)
            r (mod  x 32)]
        (if (> x 32)
          (recur q (str u (nth chars-b32 r)))
          (apply str (reverse (str u (nth chars-b32 r)))))))))

(defn get-current-id
  [uuid-value]
  (get (j/get-by-id h2-db "orders" uuid-value :random_token) :id))

(defn save-order
  "Guarda una orden en la base de datos y devuelve su uuid"
  ([new-order]
   (println "save order" new-order)
   (let [json-order (write-str new-order)
         uuid-order (java.util.UUID/randomUUID)]
     (j/insert! h2-db "orders" {:the_order json-order
                                :date_received (java.time.LocalDateTime/now)
                                :status "pending"
                                :random_token uuid-order
                                :short_token "0000"})
     (let [id  (get-current-id uuid-order)
	   short_token (encode-human-readable-base32 id)]
       (println id "------------")
       (j/update! h2-db :orders {:short_token short_token } ["random_token = ?" uuid-order])
       short_token))))


(defn jsondb-to-str [jsondb]
  (clojure.string/join (map #(when (not= 92 %) (char %)) jsondb)))

(defn get-order-status
  [uuid-order]
  (j/query h2-db ["select status from orders where ? = random_token" uuid-order]))

(def food (atom (let [menu-food (j/query h2-db ["select name from menu"])]
                  (map #(get % :name) menu-food))))

(defn is-order-correct? [order]
  (println (class order) order)
  (subset? (set order) (set @food)))

(defn- pretty-string-menu [food-map]
  (let [name (get food-map :name)
        price (str "\t| " (get food-map :price) "\n")]
    (str name price)))

(def food-with-prices (atom (j/query h2-db ["select name,price from menu"])))

(defn get-all-orders []
  (map #(assoc % :the_order (jsondb-to-str (:the_order % ))) (j/query h2-db ["select * from orders"])))

(defn get-order
  [order-token]
  (j/query h2-db ["select * from orders where ? = random_token " order-token]))

(defn order-to-cook
  [uuid-order]
  (j/update! h2-db :orders {:status "cooking"} ["random_token = ?" uuid-order]))

(defn order-to-deliver
  [uuid-order]
  (j/update! h2-db :orders {:status "in delivery"} ["random_token = ?" uuid-order]))

(defn order-to-refuse
  [uuid-order]
  (j/update! h2-db :orders {:status "refused"} ["random_token = ?" uuid-order]))

(defn order-to-pending
  [uuid-order]
  (j/update! h2-db :orders {:status "pending"} ["random_token = ?" uuid-order]))

(defn order-to-be-done
  [uuid-order]
  (j/update! h2-db :orders {:status "done"} ["random_token = ?" uuid-order]))
