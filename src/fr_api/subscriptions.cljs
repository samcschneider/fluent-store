(ns fr-api.subscriptions
  (:require [re-frame.core :as re]))

(re/reg-sub

  :ecom/cart-items

  (fn [db]
    (vals (:cart-items db))))

(re/reg-sub

  :ecom/cart-items-count

  (fn [_ _]
    (re/subscribe [:ecom/cart-items])
    )

  (fn [cart-items _]
    (reduce + (map :quantity cart-items))
    )

  )

(re/reg-sub

  :ecom/cart-subtotal

  (fn [_ _]
    (re/subscribe [:ecom/cart-items])
    )

  (fn [cart-items]
    (reduce + (map (fn [item] (* (:price item) (:quantity item))) cart-items))
    )

  )

(re/reg-sub

  :ecom/cart-total

  ;(fn [_ _]
  ;  (re/subscribe [:ecom/cart-items])
  ;  )
  ;
  ;;[:ecom/cart-items]

  (fn [db]
    (let [cart-items (vals (:cart-items db))]
      (+ (reduce + (map (fn [item] (* (:price item) (:quantity item))) cart-items))
         (or (get-in db [:cart :shipping :cost]) 0))
      ))

  )

(re/reg-sub

  :ecom/sku-quantity

  (fn [_ _]
    (re/subscribe [:ecom/cart-items])
    )

  [:ecom/cart-items]

  (fn [cart-items _]
    (vec (map (fn[item] {"sku" (:sku item) "quantity" (:quantity item)}) cart-items))
    )
)

(re/reg-sub

  :ecom/last-order-id

  (fn [db]
    (:last-order db)
    )
  )

(re/reg-sub

  :ecom/locations

  (fn [db]
    (get-in db [:locations :results])
    )
  )

(re/reg-sub

  :ecom/warehouses

  (fn [db]
    (filter (get-in db [:locations :results]) (fn[location] (= "WAREHOUSE" (:type location)) ))
    )
  )

(re/reg-sub

  :ecom/stores

  (fn [db]
    (filter (get-in db [:locations :results]) (fn[location] (= "STORE" (:type location)) ))
    )
  )



(re/reg-sub

  :ecom/available-for-hd

  (fn [db]
    (= (get-in db [:warehouse-inventory :coverage]) "ALL")
    )
  )

(re/reg-sub

  :ecom/available-for-cc

  (fn [db]
    (= (get-in db [:store-inventory :coverage]) "ALL")
    )
  )

(re/reg-sub

  :ecom/variant-selectors

  (fn [db]
    (:variant-selectors db)
    )
  )

(re/reg-sub

  :ecom/replacement-sku

  ;TODO make this a vector of maps or something
  (fn [db]
    (:replacement-sku db)
    )
  )


(re/reg-sub

  :ecom/order-error

  (fn [db]
    (:order-error db)
    )
  )

(re/reg-sub

  :ecom/order-history

  (fn [db]
    (get-in db [:order-history :results])
    )
  )

(re/reg-sub

  :ecom/order-details

  (fn [db]
    (:order-details db)
    )
  )

(re/reg-sub
  :ecom/order-subtotal

  (fn [db]
    (let [order (:order-details db)
          items (:items order)]
      (reduce + (map (fn [item] (* (:requestedQty item) (:skuPrice item))) items))
      )
    )
  )

(re/reg-sub

  :ecom/sites

  (fn [db]
    (:sites db)
    )
  )

(re/reg-sub

  :ecom/active-site-abbreviation

  (fn [db]
    (if-let [site-name (get-in db [:current-site :name])]
      (apply str (map first (clojure.string/split site-name #"\s")))
      ""
      )
    )
  )

(re/reg-sub

  :ecom/categories

  (fn [db]
    (vals (get-in db [:current-site :categories]))
    )
  )


(re/reg-sub

  :ecom/top-level-categories

  (fn [_ _]
    (re/subscribe [:ecom/categories])
    )

  [:ecom/categories]

  (fn [categories _]
    (filter #(nil? (:parent %)) categories )
    )

  )

(re/reg-sub
  :ecom/category-by-parent

  (fn [db [e parent]]
    (let [categories (vals (get-in db [:current-site :categories]))]
      (vec (filter #(= (:parent %) parent) categories))
    )
    )
  )

(re/reg-sub
  :ecom/products-by-supercategory

  (fn [db [e category-id]]
    (let [all-products (vals (get-in db [:current-site :products]))
          cat-key (if (clojure.string/starts-with? category-id ":") (symbol (clojure.string/trim category-id)) category-id)
          category (get-in db [:current-site :categories cat-key])
          cat-keys (keys (get-in db [:current-site :categories]))
          ]
      (filter #(contains? (set (:supercategories %)) (:ref category)) all-products)
      )
    )
  )

(re/reg-sub
  :ecom/products-by-ref

  (fn [db [_ product-ref]]
    (let [all-products (vals (get-in db [:current-site :products]))]
      (if all-products
        (let [product (first (filter #(= (:ref %) product-ref) all-products))]
          (if (nil? product)
            {}
            product
            )
          )
        {}
        )
      )
    )
  )

(re/reg-sub
  :ecom/order-paging

  (fn [db]
    (let [order-history (:order-history db)]
      (if order-history
        ;todo destructure
        (let [start (:start order-history)
              count (:count order-history)
              total (:total order-history)
              ]
          {:has-next (< (+ start count) total)
           :has-prev (> start count)
           :current-page (+ 1 (quot start count))
           :total total
           :total-pages (+ 1 (quot total count))
           :next-page (+ start count)
           :prev-page (- start count)}
          )

        {:has-next false :has-prev false :total 0 :current-page 0}

        )
      )
    )
  )

(re/reg-sub

  :ecom/site-users

  (fn [db]
    (vals (get-in db [:current-site :customers]))
    )
  )

(re/reg-sub

  :ecom/current-user

  (fn [db]
    (:current-user db)
    )
  )

(re/reg-sub

  :ecom/logged-in?

  (fn [db]
    (let [current-user (:current-user db)]
      (and current-user (not= (:firstname current-user) "anonymous"))
      )
    )
  )

(re/reg-sub

  :ecom/widget-config

  (fn [db]
    ;TODO multiple environment support
    (if-let [environment (first (vals (get-in db [:current-site :environments])))]
      (if-let [widget (:widget environment)]
        widget
        (println "No widget config found!" )
        )
      (println "No environment found!")
      )
    )
  )


(re/reg-sub

  :ecom/home-page-content

  (fn [db]
    (if-let [content (get-in db [:current-site :config :content :home])]
      (do
        (println "Found home page content " content)
        (let [parsed (cljs.reader/read-string content)]
          (println "Parsed content " parsed)
          parsed
          )
        )
      (do
       ; (println "No home page content")
        []
       )
      )
    )
  )

(re/reg-sub

  :ecom/shipping-cost

  (fn [db]
    (or (get-in db [:cart :shipping :cost]) 0)
    )
  )

(re/reg-sub

  :ecom/current-page

  (fn [db]
    (or (get-in db [:current-page]) {:id :home})
    )
  )

(re/reg-sub

  :ecom/current-page-args

  (fn [db]
    (or (get-in db [:current-page :args]) {})
    )
  )

(re/reg-sub

  :ecom/current-page-id

  (fn [db]
    (or (get-in db [:current-page :id]) :home)
    )
  )


(re/reg-sub

  :ecom/selected-site-abbrev

  (fn [db]
    ;TODO maybe split and extract the first of each word?
    (clojure.string/upper-case (first (get-in db [:current-site :name])))
    )
  )

(re/reg-sub
  :ecom/forms

  (fn [db [_ document element]]
    (get-in db [:forms document element])
    )
  )

(re/reg-sub

  :ecom/delivery-type

  (fn [db]
    (:delivery db)
    )
  )