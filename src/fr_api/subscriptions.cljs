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
    (if-let [site-name (get-in db [:config :name])]
      (apply str (map first (clojure.string/split site-name #"\s")))
      ""
      )
    )
  )

(re/reg-sub

  :ecom/categories

  (fn [db]
    (vals (get-in db [:config :categories]))
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
    (let [categories (vals (get-in db [:config :categories]))]
      (vec (filter #(= (:parent %) parent) categories))
    )
    )
  )

(re/reg-sub
  :ecom/products-by-supercategory

  (fn [db [e category-id]]
    (let [all-products (vals (get-in db [:config :products]))
          category (get-in db [:config :categories category-id])]
      (filter #(contains? (set (:supercategories %)) (:ref category)) all-products)
      )
    )
  )

(re/reg-sub
  :ecom/products-by-ref

  (fn [db [_ product-ref]]
    (let [all-products (vals (get-in db [:config :products]))]
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

        {:has-next false :has-pref false :total 0 :current-page 0}

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
    (clojure.string/upper-case (first (get-in db [:config :name])))
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