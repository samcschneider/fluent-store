(ns fr-ecom.views
  (:require
    [fr-ecom.components :as c]
    [fr-api.data-source :as ds]
    [cljs.pprint :as pp]
    [re-frame.core :as re :refer [subscribe dispatch]]
    )
  )

(defn home[] ;c/categories c/products-men c/divider c/products-women c/brands
  [:div [c/header'] [c/hero-items] c/example-modal]
  )

(defn categories[]
  (println "views :: Rending product-list")
  [:div [c/header'] [c/product-list] c/example-modal]
  )

(defn orders[]
  (let [user @(subscribe [:ecom/current-user])]
  [:div [c/header'] [c/section-header "Order History" "Orders" (str "Recent orders for " (:firstname user) " " (:lastname user))] [c/order-list]]
    )
  )

(defn order-details[]
  (println "Viewing order-details")
  [:div [c/header'] [c/details-header] [c/order-details]])

(defn hello[]
  [:div [:p "hello"] c/hero-items]
  )

(defn product-details [app]
  (println "views :: Rending product details...")
  (.scrollTo js/window 0 0) ;scroll window to top in case we're coming from a list page
  (let [product-ref (:product-ref @(subscribe [:ecom/current-page-args]))
        base-product @(subscribe [:ecom/products-by-ref product-ref]) ; (ds/find-product-by-id product-id)
        variant-selector-id (:variant-selector base-product)
        variant-selectors @(subscribe [:ecom/variant-selectors])
        variant-selector (get variant-selectors variant-selector-id)
        ]

    (println (str "ref" product-ref " base product: " base-product " variant selector: " variant-selector-id))
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
  [:div [c/header'] [c/product-detail app]]
  )

(defn cart []
  [:div [c/header'] [c/section-header "Shopping Cart" "Cart"] [c/cart-contents]]
  )

(defn delivery[]
  [:div [c/header'] [c/section-header "Delivery Option" "Checkout / Delivery"] [c/delivery]])

(defn store[]
  [:div [c/header'] [c/store]])

(defn checkout-availability[]
  [:div [c/header'] [c/section-header "Availability" "Checkout / Availability"] [c/availability]])

(defn checkout[]
  [:div [c/header'] [c/section-header "Checkout" "Checkout / Address"] [c/checkout]])

(defn checkout-shipping[]
  [:div [c/header'] [c/section-header "Shipping" "Checkout / Shipping"] [c/shipping]])

(defn checkout-payment[]
  [:div [c/header'] [c/section-header "Payment" "Checkout / Payment"] [c/payment]])

(defn checkout-summary[]
  [:div [c/header'] [c/section-header "Order Review" "Checkout / Review"] [c/cart-review]])

(defn order-confirmation[]
  [:div [c/header'] [c/order-confirmation]])
