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
    [clojure.string :as string]
    [fr-api.db]
    [fr-api.events :as ev]
    [re-frame.core :as re-frame :refer [dispatch subscribe]]
    ))

(enable-console-print!)


(defonce app-state (reagent/atom
                     {:orders {}
                      :cart-add 2
                      :cart {}
                      :shipping 0.00
                      :replacement ds/replacement
                      :address {}
                      :payment {}
                      :config {}
                      }))

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

;(def sites-chan (chan))
;
;(defn sites-event-loop []
;  (go-loop []
;           (when-let [response (<! sites-chan)]
;             (net/log "received data on sites channel")
;             (net/log response)
;             (if (= (:status response) 200)
;               (let [sites (:body response)
;                     selected (filter (fn [i] (= (:name i) ds/default-site)) (:sites sites))]
;                 (swap! app-state merge @app-state sites)
;                 (let [site (if selected
;                              (first selected)
;                              ;otherwise, just default ot the first one?
;                              (first (:sites sites)))]
;                   (comp/change-site (:id site) app-state)
;                   )
;                 )
;               (swap! app-state merge @app-state {:sites-error (format-error response)})
;               )
;             (recur)
;             )
;           )
;  )

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
            (dispatch [:ecom/navigate :home])
            )

  (defroute "/categories/:category-id" [category-id]
            (dispatch [:ecom/navigate :categories {:category-id category-id}])
            )

  (defroute "/hello" []
            (dispatch [:ecom/navigate :hello]))

  (defroute "/cart" []
            (dispatch [:ecom/navigate :cart]))

  (defroute "/checkout-delivery" []
            (dispatch [:ecom/navigate :checkout.delivery]))

  (defroute "/checkout-store" []
            (dispatch [:ecom/navigate :checkout.store])
            )

  (defroute "/checkout-availability" []
            (dispatch [:ecom/navigate :checkout.availability])
            )

  (defroute "/checkout-address" []
            (dispatch [:ecom/navigate :checkout.address])
            )

  (defroute "/checkout-shipping" []
            (dispatch [:ecom/navigate :checkout.shipping])
            )

  (defroute "/checkout-payment" []
            (dispatch [:ecom/navigate :checkout.payment])
            )

  (defroute "/checkout-summary" []
            (dispatch [:ecom/navigate :checkout.summary])
            )

  (defroute "/checkout-placeorder" []
            (dispatch [:ecom/navigate :checkout.placeorder])
            )

  (defroute "/product-details/:product-ref" [product-ref]
            (dispatch [:ecom/navigate :product.details {:product-ref product-ref}])
            )

  (hook-browser-navigation!))

(defmulti current-page #(:id @(subscribe [:ecom/current-page])))

(defmethod current-page :home []
  [v/home])

(defmethod current-page :categories []
  [v/categories])

(defmethod current-page :hello []
  [v/hello])

(defmethod current-page :cart []
  [v/cart])

(defmethod current-page :product.details []
  [v/product-details app-state])

(defmethod current-page :checkout.delivery []
  [v/delivery])

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

(defn setup-ui []
  (orders-event-loop)
  (app-routes)
  current-page
  )

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render-component [setup-ui]
                            (. js/document (getElementById "app"))))

(defn ^:export init []
  (re-frame/dispatch-sync [:ecom/initialize-db])
  ;TODO lazy start these event loops?
  (ev/new-sites-event-loop :ecom/sites-received :ecom/sites-error)
  (ev/config-event-loop :ecom/config-received :ecom/config-error)
  (ev/place-order-event-loop)
  (ev/fulfilment-options-event-loop)
  (re-frame/dispatch-sync [:ecom/load-sites])
  ;(dev-setup)
  (mount-root))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
