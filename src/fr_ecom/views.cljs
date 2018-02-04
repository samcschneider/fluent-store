(ns fr-ecom.views
  (:require
    [fr-ecom.components :as c]
    [fr-api.data-source :as ds]
    [cljs.pprint :as pp]
    )
  )

(defn home[app]
  [:div [c/header' app] c/hero-items c/categories c/products-men c/divider c/products-women c/brands c/example-modal]
  )

(defn categories[app]
  (println "views :: Rending product-list")
  [:div [c/header' app] [c/product-list app] c/example-modal]
  )

(defn hello[]
  [:div c/header [:p "hello"] c/hero-items]
  )

(defn product-details [app]
  (println "views :: Rending product details...")
  (.scrollTo js/window 0 0) ;scroll window to top in case we're coming from a list page
  (let [product-id (get-in @app [:args :product-id])
        base-product (ds/find-product-by-id product-id);(first (filter #(= product-id (:id %)) (:catalog @app)))
        variant-selector-id (:variant-selector base-product)
        variant-selector (get (:variant-selector @app) variant-selector-id)
        ]

  (when variant-selector-id
    (let [selector-ids (map :id (filter :selector? variant-selector))
          selector-state (reduce (fn [m v] (assoc m v "")) {} selector-ids)
          app-selector-state (get-in @app [:variant-state variant-selector-id])]
      (println selector-ids)
      (println selector-state)
      ;TODO remove this hack - component will render twice here - re-frame to the rescue?
      (when (nil? app-selector-state) ; (not= selector-state app-selector-state))
        (println "views :: product-details :: Setting new variant-state")
        (swap! app assoc-in [:variant-state variant-selector-id] selector-state))
      )
    ))
  [:div [c/header' app] [c/product-detail app]]
  )

(defn cart [app]
  ;(.log js/console (with-out-str (pp/pprint app )) )
  ;(.log js/console (with-out-str (pp/pprint (:cart app))))
  ;[:div c/header c/checkout-header c/checkout]
  [:div [c/header' app] [c/section-header app "Shopping Cart" "Cart"] [c/cart-contents app]]
  )

(defn delivery[app]
  [:div [c/header' app] [c/section-header app "Delivery Option" "Checkout / Delivery"] [c/delivery app]])

; [:div [c/header' app] [c/section-header app "Choose Store" "Checkout / Store"] [c/store]])

(defn store[app]
  [:div [c/header' app] [c/store app]])

(defn checkout-availability[app]
  [:div [c/header' app] [c/section-header app "Availability" "Checkout / Availability"] [c/availability app]])

(defn checkout[app]
  [:div [c/header' app] [c/section-header app "Checkout" "Checkout / Address"] [c/checkout app]])

(defn checkout-shipping[app]
  [:div [c/header' app] [c/section-header app "Shipping" "Checkout / Shipping"] [c/shipping app]])

(defn checkout-payment[app]
  [:div [c/header' app] [c/section-header app "Payment" "Checkout / Payment"] [c/payment app]])

(defn checkout-summary[app]
  [:div [c/header' app] [c/section-header app "Order Review" "Checkout / Review"] [c/cart-review app]])

(defn order-confirmation[app]
  [:div [c/header' app] [c/order-confirmation app]])
