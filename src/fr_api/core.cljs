(ns fr-api.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]] [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require
    [reagent.core :as reagent]
    [secretary.core :as secretary]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [fr-api.network :as net]
    [cljs.core.async :refer [<! put! chan]]
    [cljs.pprint :as pp]
    [fr-ecom.views :as v]
    [fr-ecom.components :as comp]
    [fr-api.data-source :as ds]
    [clojure.string :as string]))

(enable-console-print!)

(println "This text is printed from src/fr-api/core.cljs. Go ahead and edit it and see reloading in action.")

(declare next-cart-key!)

(defonce app-state (reagent/atom
                     {:orders {}
                      :categories ds/categories
                      :variant-selector ds/variant-selectors
                      :cart-add 0
                      :cart {}
                      :shipping 0.00
                      :replacement ds/replacement
                      :address {}
                      :saved-address ds/saved-address
                      :payment {}
                      :saved-payment ds/saved-payment
                      :config {}
                      :next-key-fn #(next-cart-key!)
                      }))

(defn next-cart-key![]
  (let [key (+ 1 (get @app-state :cart-add 0))]
    (swap! app-state assoc :cart-add key)
    key
    )
  )

(def orders-chan (chan))

(defn get-orders[]
  (net/log "Getting orders...")
  (net/get-orders orders-chan)
  )

;TODO move to common utils along with log
(defn format-error [response]
  (str "Error returned from server " (:status response) " " (get-in response [:body :message]))
  )

(defn orders-event-loop []
  (go-loop []
           (when-let [response (<! orders-chan)]
             (net/log "received data on orders channel")
             (net/log response)
             (if (= (:status response) 200)
               (swap! app-state merge @app-state {:orders (:body response)})
               (swap! app-state merge @app-state {:orders (format-error response)})
               )
             (recur)
             )
           )
  )

(def locations-chan (chan))

(defn locations-event-loop []
  (go-loop []
           (when-let [response (<! locations-chan)]
             (net/log "received data on locations channel")
             (net/log response)
             (if (= (:status response) 200)
               (swap! app-state merge @app-state {:locations (:body response)})
               (swap! app-state merge @app-state {:locations (format-error response)})
               )
             (recur)
             )
           )
  )

(def sites-chan (chan))

(defn sites-event-loop []
  (go-loop []
           (when-let [response (<! sites-chan)]
             (net/log "received data on sites channel")
             (net/log response)
             (if (= (:status response) 200)
               (let [sites (:body response)
                     selected (filter (fn [i] (= (:name i) ds/default-site)) (:sites sites))]
                 (swap! app-state merge @app-state sites)
                 (let [site (if selected
                              (first selected)
                              ;otherwise, just default ot the first one?
                              (first (:sites sites)))]
                   (comp/change-site (:id site) app-state)
                   )
                 )
               (swap! app-state merge @app-state {:sites-error (format-error response)})
               )
             (recur)
             )
           )
  )

;temp hack to cache all active locations
(defn get-locations[]
  (net/log "Getting locations...")
  (net/get-all-locations locations-chan)
  )

(defn find-location[locationRef]
  (filter #(= (:locationRef %) locationRef) (get-in @app-state [:locations :results])))

(defn token-ui[state]
  [:div
   [:h1 [:p @state]]
   [:div
    [:button {:on-click #(net/renew-token)} "Get Token"]]])

(defn get-orders-ui [state]
  [:div
   [:h1 "Orders response: " [:textarea {:value (with-out-str (pp/pprint (:orders @state))) :cols 120 :rows 15}]]
   [:div
    [:button {:on-click #(get-orders)} "Get Orders"]]])

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")

  (defroute "/" []
            (swap! app-state assoc :page :home))

  (defroute "/categories/:category-id" [category-id]
            (js/console.log (str "category id: " category-id))
            ;TODO hate this... there has to be a cleaner way to get query params and path args
            ;to the dispatched methods?!
            (swap! app-state assoc :args {:category-id category-id})
            (swap! app-state assoc :page :categories))

  (defroute "/hello" []
            (swap! app-state assoc :page :hello))

  (defroute "/cart" []
            (swap! app-state assoc :page :cart))

  (defroute "/checkout-delivery" []
            (swap! app-state assoc :page :checkout.delivery))

  (defroute "/checkout-store" []
            (swap! app-state assoc :page :checkout.store))

  (defroute "/checkout-availability" []
            (swap! app-state assoc :page :checkout.availability))

  (defroute "/checkout-address" []
           (swap! app-state assoc :page :checkout.address))

  (defroute "/checkout-shipping" []
            (swap! app-state assoc :page :checkout.shipping))

  (defroute "/checkout-payment" []
            (swap! app-state assoc :page :checkout.payment))

  (defroute "/checkout-summary" []
            (swap! app-state assoc :page :checkout.summary))

  (defroute "/checkout-placeorder" []
            (swap! app-state assoc :page :checkout.placeorder))

  (defroute "/product-details/:product-id" [product-id]
            (js/console.log (str "product id: " product-id))
            ;TODO hate this... there has to be a cleaner way to get query params and path args
            ;to the dispatched methods?!
            (swap! app-state assoc :args {:product-id product-id})
            (swap! app-state assoc :page :product.details))

  (hook-browser-navigation!))

(defmulti current-page #(@app-state :page))

(defmethod current-page :home []
  [v/home app-state])

(defmethod current-page :categories []
  (println "Rendering :: categories")
  [v/categories app-state])

(defmethod current-page :hello []
  [v/hello])

(defmethod current-page :cart []
  [v/cart app-state])

(defmethod current-page :product.details []
  [v/product-details app-state])

(defmethod current-page :checkout.delivery []
  [v/delivery app-state])

; cc option
(defmethod current-page :checkout.store []
  [v/store app-state])

(defmethod current-page :checkout.availability []
  [v/checkout-availability app-state])

;TODO make collection view...
; select cc location based on fulfillment options
;(defmethod current-page :checkout.collection[]
;  [v/collection app-state find-location] ; pass in location finder function?
;  )

; hd option
(defmethod current-page :checkout.address []
  [v/checkout app-state])

(defmethod current-page :checkout.shipping []
  [v/checkout-shipping app-state])

(defmethod current-page :checkout.payment []
  [v/checkout-payment app-state])

(defmethod current-page :checkout.summary []
  [v/checkout-summary app-state])

(defmethod current-page :checkout.placeorder []
  [v/order-confirmation app-state])

(defn setup-ui' []

  ;TODO introduce app namespace? Comp --> App <-- core --> Comp
  (comp/config-event-loop app-state next-cart-key!)
  (sites-event-loop)
  (net/fr-get "http://localhost:8890/site" {} sites-chan)
  (orders-event-loop)
  (locations-event-loop)
  (comp/fulfilment-options-event-loop app-state)
  (comp/place-order-event-loop app-state)
  (get-locations)
  (app-routes)
  current-page
  )

(reagent/render-component [setup-ui']
                          (. js/document (getElementById "app")))
;(defn ^:export main []
;  (app-routes)
;  (reagent/render [current-page]
;                  (.getElementById js/document "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
