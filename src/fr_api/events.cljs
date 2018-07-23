(ns fr-api.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as rf]
            [fr-api.db :as db]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [cljs.core.async :refer [<! put! chan]]
            [fr-api.network :as net]
            [fr-api.data-source :as ds]
            [cljs.pprint :as pp]
            ))

(defn next-id
  "Returns the next cart item id.
  Assumes cart items are sorted.
  Returns one more than the current largest id."
  [cart-items]
  ((fnil inc 0) (last (keys cart-items))))

(rf/reg-event-db
  :ecom/initialize-db
  (fn-traced [_ _]
             db/default-db))

(rf/reg-event-db
  :ecom/add-to-cart

  [(rf/path :cart-items)]                                   ;ditch db arg and just use the path interceptor to narrow db

  (fn [cart-items [_ item quantity]]
    (let [id (next-id cart-items)]
      (assoc cart-items id (merge item {:key id :quantity quantity})))))

(rf/reg-event-db
  :ecom/change-quantity

  [(rf/path :cart-items)]                                   ;ditch db arg and just use the path interceptor to narrow db

  (fn [cart-items [_ item-key operation]]
    (let [cart-item (get cart-items item-key)
          quantity (operation (:quantity cart-item))
          remove? (<= quantity 0)
          ]
      (if remove?
        (dissoc cart-items item-key)
        (assoc cart-items item-key (assoc cart-item :quantity quantity))
        )
      )
    )
  )

(rf/reg-event-db
  :ecom/remove-cart-item

  [(rf/path :cart-items)]                                   ;ditch db arg and just use the path interceptor to narrow db

  (fn [cart-items [_ item-key]]
    (dissoc cart-items item-key)
    )
  )

(rf/reg-event-db

  :ecom/sites-received

  (fn [db [_ response]]
    (assoc db :sites (filter #(not= (:name %1) "CREATE") (:sites response)))
    )

  )

(rf/reg-event-db

  :ecom/sites-error

  (fn [db [_ response]]
    (assoc db :sites-error response)
    )

  )

(def locations-chan (chan))

(defn locations-event-loop
  [success failure]
  (go-loop []
           (when-let [response (<! locations-chan)]
             (net/log "received data on locations channel")
             (net/log response)
             (let [body (:body response)]
               (if (= (:status response) 200)
                 (rf/dispatch [success body])
                 (rf/dispatch [failure body])
                 ))
             (recur)
             )
           )
  )

(def warehouse-inventory-chan (chan))

(defn warehouse-inventory-event-loop
  [success failure]
  (go-loop []
           (when-let [response (<! warehouse-inventory-chan)]
             (net/log "received data on locations channel")
             (net/log response)
             (let [body (:body response)]
               (if (= (:status response) 200)
                 (rf/dispatch [success body])
                 (rf/dispatch [failure body])
                 ))
             (recur)
             )
           )
  )

(def store-inventory-chan (chan))

(defn store-inventory-event-loop
  [success failure]
  (go-loop []
           (when-let [response (<! store-inventory-chan)]
             (net/log "received data on locations channel")
             (net/log response)
             (let [body (:body response)]
               (if (= (:status response) 200)
                 (rf/dispatch [success body])
                 (rf/dispatch [failure body])
                 ))
             (recur)
             )
           )
  )

(rf/reg-event-db
  :ecom/store-inventory-received
  (fn [db [_ response]]
    (assoc db :store-inventory response)
    )
  )

(rf/reg-event-db
  :ecom/store-inventory-error
  (fn [db [_ response]]
    (assoc db :store-inventory {:error response})
    )
  )

(rf/reg-event-db
  :ecom/warehouse-inventory-received
  (fn [db [_ response]]
    (assoc db :warehouse-inventory response)
    )
  )

(rf/reg-event-db
  :ecom/warehouse-inventory-error
  (fn [db [_ response]]
    (assoc db :warehouse-inventory {:error response})
    )
  )

(rf/reg-event-db
  :ecom/locations-received
  (fn [db [_ response]]
    (assoc db :locations response)
    )
  )

;{:locationType "WAREHOUSE" :collectionType CUSTOMER" :locationRefs ["ES101"] :orderItems [{:skuRef "MJS-001" :requestedQty" 5}]}
(rf/reg-event-fx

  :ecom/check-inventory

  (fn [cofx [_ sku-ref]]
    (let [db (:db cofx)
          locations (get-in db [:locations :results])
          loc-by-type (group-by :type locations)
          store-sku-request {:skuRef sku-ref :requestedQty 1}
          warehouse-sku-request {:skuRef sku-ref :requestedQty 10}
          stores (map :locationRef (get loc-by-type "STORE"))
          warehouses (map :locationRef (get loc-by-type "WAREHOUSE"))
          base-args {:collectionType "CUSTOMER"}]

      (net/get-inventory (merge base-args {:locationType "WAREHOUSE" :orderItems [warehouse-sku-request]
                                           :locationRefs warehouses}) warehouse-inventory-chan)
      (net/get-inventory (merge base-args {:locationType "STORE" :orderItems [store-sku-request]
                                           :locationRefs stores}) store-inventory-chan)
      {}
      )
    )
  )


(rf/reg-event-db
  :ecom/locations-error
  (fn [db [_ response]]
    (assoc db :locations {:error response})
    )
  )

(rf/reg-event-fx
  :ecom/load-locations
  (fn [coeffects event]
    (net/log "loading locations from event...")
    ;TODO return an http effect instead of calling API directly and returning empty coeffects
    ;(net/fr-get "http://localhost:8890/site" {} new-sites-chan)
    (net/get-all-locations locations-chan)
    {}
    )
  )

(def new-sites-chan (chan))

(rf/reg-event-fx
  :ecom/load-sites
  (fn [coeffects event]
    (net/log "loading sites from event...")
    ;TODO return an http effect instead of calling API directly and returning empty coeffects
    ;(net/fr-get "http://localhost:8890/site" {} new-sites-chan)
    (net/fr-get "http://ec2-13-57-56-125.us-west-1.compute.amazonaws.com:9191/site" {} new-sites-chan)
    {}
    )
  )

(def config-chan (chan))

(defn to-set [data key-val]
  (if (key-val data)
    (assoc data key-val (set (key-val data)))
    data
    )
  )

(defn build-catalog [catalog]

  (map (fn [item]
         (println (str "processing..." item))
         (let [key (first item)
               product (second item)]
           (-> product
               (to-set :supercategories)
               (to-set :variant-cats)
               (assoc :id key)
               (assoc :sku (or (:sku product) (when (not (:base product)) (:ref product))))
               )
           )) catalog)

  )

(defn config-event-loop
  [success failure]
  (go-loop []
           (when-let [response (<! config-chan)]
             (net/log "received data on config channel")
             (net/log response)
             (let [body (:body response)]
               (if (= (:status response) 200)
                 (rf/dispatch [success body])
                 (rf/dispatch [failure body])
                 ))
             (recur)
             )
           )
  )

(rf/reg-event-db
  :ecom/config-received
  (fn [db [_ response]]
    (reset! ds/catalog (build-catalog (:products response)))
    ;TODO figure out what to do with ds...
    (reset! ds/categories (vals (:categories response)))


    ;TODO select environment based on :config :env :default instead

    (if-let [environment (first (vals (:environments response)))]
      (reset! net/env environment)
      (println "No environment found!")
      )
    (rf/dispatch [:ecom/load-locations])
    (assoc db :current-site response)
    )
  )



(rf/reg-event-db

  :ecom/navigate

  (fn [db [_ id args]]
    (println (str "Changing page: " id " with args: " args))
    (assoc db :current-page {:id id :args args})
    )
  )

(rf/reg-event-db

  :ecom/set-form-value

  (fn [db [_ document element value]]
    (assoc-in db [:forms document element] value)
    )
  )

(rf/reg-event-db

  :ecom/update-delivery-address

  (fn [db [_ value]]
    (net/log (str "Updating address: " value))
    (assoc-in db [:forms :address] value)
    )
  )

(rf/reg-event-db

  :ecom/use-saved-delivery-address

  (fn [db [_ value]]
    (println (println "Address: "(get-in db [:current-user :shipping-address])))
    (assoc-in db [:forms :address] (get-in db [:current-user :shipping-address]))
    )
  )



(rf/reg-event-db

  :ecom/switch-user

  (fn [db [_ user]]
    (println (str "Payment: "(get-in db [:current-user :payment])))
    (assoc db :current-user user)
    )
  )

(rf/reg-event-db

  :ecom/use-saved-payment

  (fn [db [_ value]]
    (println (str "Payment: "(get-in db [:current-user :payment])))
    (assoc-in db [:forms :payment] (get-in db [:current-user :payment]))
    )
  )

(rf/reg-event-db

  :ecom/update-payment

  (fn [db [_ value]]
    (net/log (str "Updating payment: " value))
    (assoc-in db [:forms :payment] value)
    )
  )

(defn- derive-name [sym]
  "transforms symbols of the form: :somename-shipping to SOMENAME"
  (clojure.string/upper-case (clojure.string/replace (str sym) #"[:|-]|shipping" ""))
  )

(rf/reg-event-db
  :ecom/update-shipping

  (fn [db [_ value cost]]
    (net/log (str "Updating shipping to: " value))
    (let [shipping-cost (cond
                          (= value :standard-shipping) 5
                          (= value :express-shipping) 20
                          :else (or cost 0)                 ;unknown
                          )]
      (assoc-in db [:cart :shipping] {:method value :cost shipping-cost :name (derive-name value)})
      )
    )
  )

(rf/reg-event-db
  :ecom/delivery-method

  (fn [db [_ method]]
    (net/log (str "Updating delivery method to: " method))
    (when (= method :CC)
      (rf/dispatch [:ecom/update-shipping :CC 0])
      )
    (assoc db :delivery method)
    )
  )

(rf/reg-event-db
  :ecom/order-placed

  (fn [db [_ order-id]]
    (-> db
        (assoc :last-order order-id)
        (assoc :cart-items {})
        (assoc-in [:forms :address] {})
        (assoc-in [:forms :payment] {})
        (assoc :delivery nil)
        (assoc-in [:cart :shipping] {})
        )
    )
  )


(rf/reg-event-db
  :ecom/order-error

  (fn [db [_ error]]
    (assoc db :order-error error)
    )
  )


(rf/reg-event-fx
  :ecom/load-site-config
  (fn [cofx [_ site-id]]
    (net/log (str "Changing site to " site-id))
    ;TODO return an http/API effect instead of calling API directly and returning empty coeffects
    (net/fr-get (str "http://ec2-13-57-56-125.us-west-1.compute.amazonaws.com:9191/site/" site-id) {} config-chan)
    ;(net/fr-get (str "http://localhost:8890/site/" site-id) {} config-chan)
    {}
    )
  )

;TODO use cofx to grab stored cart data - hmm.... what about storing the cart in the first place?
;(reg-cofx               ;; registration function
;  :ecom/read-selected-store                 ;; what cofx-id are we registering
;  (fn [coeffects _]    ;; second parameter not used in this case
;    (assoc coeffects :now (js.Date.))))   ;; add :now key, with value

(defn format-error [response]
  (str "Error returned from server " (:status response) " " (get-in response [:body :message]))
  )

(def orders-chan (chan))

;(defn get-orders[]
;  (net/log "Getting orders...")
;  (net/get-orders "sam" "schneider" orders-chan)
;  )

(rf/reg-event-fx
  :ecom/load-orders
  ;expecting args: first-name, last-name, count, start
  (fn [cofx [_ args]]
    ;TOOD grab customer context and inject parameters instead of event parms?
    ;TODO return an http/API effect instead of calling API directly and returning empty coeffects
    (net/get-orders args orders-chan)
    {}
    )
  )

(defn orders-event-loop [success failure]
  (go-loop []
           (when-let [response (<! orders-chan)]
             (net/log "received data on orders channel")
             (net/log response)
             (if (= (:status response) 200)
               (rf/dispatch [success (:body response)])
               (rf/dispatch [failure (:body response)])
               )
             (recur)
             )
           )
  )

(rf/reg-event-db
  :ecom/orders-error

  (fn [db [_ error]]
    (assoc db :order-history-error error)
    )
  )

(rf/reg-event-db
  :ecom/orders-received

  (fn [db [_ orders]]
    (assoc db :order-history orders)
    )
  )

(def order-details-chan (chan))

(rf/reg-event-fx
  :ecom/view-order-details
  (fn [cofx [_ order-id]]
    ;TOOD grab customer context and inject parameters instead of event parms?
    ;TODO return an http/API effect instead of calling API directly and returning empty coeffects
    (net/get-order-details order-id order-details-chan)
    {}
    )
  )

(defn order-details-event-loop [success failure]
  (go-loop []
           (when-let [response (<! order-details-chan)]
             (net/log "received data on order-details channel")
             (net/log response)
             (if (= (:status response) 200)
               (rf/dispatch [success (:body response)])
               (rf/dispatch [failure (:body response)])
               )
             (recur)
             )
           )
  )

(rf/reg-event-db
  :ecom/order-details-error

  (fn [db [_ error]]
    (assoc db :order-details-error error)
    )
  )

(rf/reg-event-db
  :ecom/order-details-received

  (fn [db [_ order-details]]
    (println "Received order details")
    (assoc db :order-details order-details)
    )
  )

(defn new-sites-event-loop
  "Pass in the effect to trigger with the data received on the channel"
  [success failure]
  (go-loop []
           (when-let [response (<! new-sites-chan)]
             (net/log "received data on **NEW** sites channel")
             (net/log response)
             (if (= (:status response) 200)
               (let [body (:body response)]
                 (rf/dispatch [success body])
                 )
               (rf/dispatch [failure (format-error response)])
               )
             (recur)
             )
           )
  )

(def place-order-chan (chan))

(defn place-order-event-loop []
  (go-loop []
           (when-let [response (<! place-order-chan)]
             (net/log "received data on place-order channel")
             (net/log response)
             (if (= (:status response) 200)
               (do
                 (rf/dispatch [:ecom/order-placed (:body response)])
                 )
               (rf/dispatch [:ecom/order-error (format-error response)])
               )
             (recur)
             )
           )
  )

(defn build-customer [first-name last-name mobile email]
  {"firstName" first-name "lastName" last-name "mobile" mobile "email" email "customerRef" "10605"}
  )

(defn build-cc-customer [payment]
  (let [pmt-field (partial get payment)
        name (pmt-field :card-name)
        name-parts (clojure.string/split name " ")
        first-name (first name-parts)
        last-name (last name-parts)
        phone (pmt-field :phone-number)
        email (pmt-field :email)]

    (build-customer first-name last-name phone email)
    )
  )

(defn build-hd-customer [address]
  (let [field-list [:firstname :lastname :phone-number :email]]
    (apply build-customer (map (partial get address) field-list))
    ))

(defn build-shipping-address [address]
  (let [adr-field (partial get address)]
    {
     "name"     (str (adr-field :firstname) " " (adr-field :lastname))
     "street"   (adr-field :street)
     "city"     (adr-field :city)
     "state"    (adr-field :state)
     "postcode" (adr-field :zip)
     "country"  (adr-field :country)
     }
    )
  )


(defn make-order [db]
  (let [payment (get-in db [:forms :payment])
        address (get-in db [:forms :address])
        delivery (:delivery db)
        ship-cost (or (get-in db [:cart :shipping :cost]) 0)
        ship-method (get-in db [:cart :shipping :name])
        customer (if (= delivery :CC) (build-cc-customer payment) (build-hd-customer address))
        selected-store (get (js->clj (aget js/window "storeAddress")) "StoreId")
        items (vec (map (fn [item] {"skuRef" (:ref item) "skuPrice" (:price item) "requestedQty" (:quantity item) "totalPrice" (* (:price item) (:quantity item))}) (vals (:cart-items db))))
        retailer-id (:retailer-id @net/env)
        ]

    (merge {
            "customer"   customer
            "items"      items
            "orderRef"   (str (rand-int 1000000))
            "retailerId" retailer-id}
           (if (= delivery :CC)
             {"fulfilmentChoice"
                     {"address" {"locationRef" selected-store}}
              "type" delivery
              }
             {; HD option...
              "fulfilmentChoice" {"deliveryType"        ship-method
                                  "fulfilmentPrice"     ship-cost
                                  "deliveryInstruction" "home delivery"
                                  "address"             (build-shipping-address address)}
              "type"             delivery
              }
             )
           )
    )
  )


(rf/reg-event-fx

  :ecom/place-order

  (fn [cofx [_ _]]
    (let [db (:db cofx)]
      (net/log "Placing order...")
      ;TODO return an http/API effect instead of calling API directly and returning empty coeffects
      (let [order (make-order db)]
        (println (with-out-str (pp/pprint order)))
        (net/place-order order place-order-chan)
        )
      {}
      )
    )
  )

;TODO implement substitution event
;(defn substitute-item[app key substitute]
;  (println (str "Subbing key: " key))
;  (let [updated (merge (get-in @app [:cart key]) substitute)]
;    (println updated)
;    (swap! app assoc-in [:cart key] updated)
;    )
;  )

(rf/reg-event-db
  :ecom/fulfillment-options

  (fn [db [_ options]]
    (assoc db :fulfillment-options options)
    )
  )

(rf/reg-event-db
  :ecom/fulfillment-options-error

  (fn [db [_ error]]
    (-> db
        (assoc :fulfillment-options {})
        (assoc :fulfillment-options-error error)
        )
    )
  )

(def fulfillment-options-chan (chan))

(defn fulfilment-options-event-loop []
  (go-loop []
           (when-let [response (<! fulfillment-options-chan)]

             (if (= (:status response) 200)
               (do
                 (rf/dispatch [:ecom/fulfillment-options (:body response)])
                 )
               (rf/dispatch [:ecom/fulfillment-options-error (:body response)])
               )
             (recur)
             )
           )
  )

(rf/reg-event-fx

  :ecom/check-fulfillment
  ;TODO add coeffect to read store from js/window

  (fn [cofx [_]]
    (let [cart-items (vec (map (fn [item] {:skuRef (:ref item) :requestedQuantity (:quantity item)}) @(rf/subscribe [:ecom/cart-items])))
          selected-store (js->clj (aget js/window "storeAddress"))
          retailer-id (:retailer-id @net/env)
          ]
      (net/log (str "Checking fulfillment"))
      ;TODO get retailer id from config... maybe a subscription for active retailer?
      (net/get-fulfillment-options (get selected-store "StoreId") cart-items retailer-id fulfillment-options-chan)
      )
    {}
    )
  )
