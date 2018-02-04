(ns fr-api.network
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [reagent.core :as reagent]
    [clojure.string :as str]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<! >!]] ;TODO bring in timeout
    )
  )

(def default-env {:client-id     "xxx"
                  :client-secret "yyy"
                  :username "zzz"
                  :password "aaa"})

(def env (atom default-env))

(def token-url (str "https://sandbox.api.fluentretail.com/oauth/token?username="
                    (:username @env) "&password="
                    (:password @env) "&scope=api&client_id="
                    (:client-id @env) "&client_secret="
                    (:client-secret @env) "&grant_type=password"))

(def bearer (reagent/atom "<no auth>"))

(defn log [msg]
  (.log js/console (str msg))
  )

(defn renew-token
  ([]
   (renew-token #())
    )
  ([retry-fn]
   (go (let [response (<! (http/post token-url
                                     {:form-params       (merge default-env {:grant_type "password"})
                                      :with-credentials? false
                                      }))]
         (let [token (:access_token (:body response))]
           (log token)
           (reset! bearer (:access_token (:body response)))
           ; retry method after a short delay
           (js/setTimeout retry-fn 200) ;TODO use <! (timeout 200)
           )
         )
       )
    )
  )

(defn auth-header[] {"Authorization"  (str "Bearer " @bearer)})

(defn default-config [headers]
  {:headers (merge headers (auth-header)) :with-credentials? false}
  )

(defn request

  ([request-fn reply-chan]
   (request request-fn reply-chan 0)
    )
  ([request-fn reply-chan tries]
   (go (let [response (<! (request-fn))]
         (if (and (>= (:status response) 200) (< (:status response) 400)) ;allow 2xx and 3xx response
           (>! reply-chan response)
           (if (or (= (:status response) 401) (= (:status response 503)))
             (do
               (log "Received a non-200 response... renewing token...")
               (let [try-count (inc tries)]
                 (if (< try-count 3)
                   (renew-token  (fn [] (request request-fn reply-chan try-count)))
                   (>! reply-chan response)
                   )
                 )
               )
             (do
               (log (str "Error response or unrecoverable status from server" response))
               (>! reply-chan response)
               )
             )
           )
         )
       )

    )
  )

(defn fr-get
  ([url headers reply-chan]
   (fr-get url headers {} reply-chan)
    )
  ([url headers params reply-chan]
   (request  (fn[] (http/get url (merge {:query-params params} (default-config headers)))) reply-chan))
  )

(defn fr-post
  ([url data headers reply-chan]
   (fr-post url data headers {} reply-chan)
    )
  ([url data headers params reply-chan]
   (request (fn [] (http/post url (merge {:query-params params :json-params data} (default-config headers)))) reply-chan)
    )
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; API endpoints and clojure API defs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def order-endpoint "https://sandbox.api.fluentretail.com/api/v4.1/order/")

;TODO grab all locations?
(def location-get-params {:agentStatuses "ACTIVE" :count 50})
(def location-endpoint "https://sandbox.api.fluentretail.com/api/v4.1/location")

(def fulfillment-options-endpoint "https://sandbox.api.fluentretail.com/api/v4.1/fulfilmentOptions")

(defn get-orders [chan]
  (fr-get order-endpoint {} chan)
  )

(defn place-order [order chan]
  (log (str "Placing order " order))
  (fr-post order-endpoint order {} chan)
  )

;assume skus -> [ { :skuRef "thesku" :requestedQuantity xx } ]
(defn get-fulfillment-options[locationRef items retailerId chan]
  (log (str "Getting fulfillment options for " locationRef items))
  (fr-post fulfillment-options-endpoint { :limit 10 :locationRef locationRef :items items :retailerId retailerId :orderType "CC"} {} chan)
  )

;fetch locations using default params (grab first 50 ACTIVE locations only)
(defn get-all-locations [chan]
  (fr-get location-endpoint {} location-get-params chan))
