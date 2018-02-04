(ns fr-ecom.components
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.pprint :as pp]
    [clojure.string :as string]
    [goog.string :as gstring]
    [goog.string.format]
    [reagent.format :as fmt]
    [clojure.string]
    [reagent.core :as reagent]
    [fr-api.network :as net]
    [fr-api.data-source :as ds]
    [cljs.core.async :refer [<! put! chan]])
  )

;TODO see check about link processing being disabled in some cases - e.g.:
;(fn [e]
;  (.(.preventDefault e)Default e)

(defn build-cart[products next-key!]
  (reduce (fn [col item]
            (let [item-key (next-key!)
                  keyed-item (assoc item :key item-key)]
              (assoc col item-key keyed-item)
              )
            ) {} products)
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

(defn config-event-loop [app next-cart-key-fn]
  (go-loop []
           (when-let [response (<! config-chan)]
             (net/log "received data on config channel")
             (net/log response)
             (when (= (:status response) 200)
               (let [config-data (:body response)]
                 (swap! app assoc :config config-data)
                 (swap! app assoc :cart (build-cart (vals (get-in @app [:config :cart])) next-cart-key-fn))
                 (reset! ds/catalog (build-catalog (get-in @app [:config :products])))
                 ;TODO should be able to remove :categories from @app...
                 (swap! app assoc :categories (vals (get-in @app [:config :categories])))
                 (reset! ds/categories (vals (get-in @app [:config :categories])))
                 )
               )
             (recur)
             )
           )
  )

(defn change-site [site-id app]
  ;TODO add config for remote host
  (when-let [selected (first (filter (fn [i] (= (:id i) site-id)) (:sites @app)))]
    (net/log (str "Selected Site --> " selected))
    (net/fr-get (str "http://localhost:8890/site/" site-id) {} config-chan)
    (swap! app assoc :site selected)
    (swap! app assoc :site-abbrev (string/upper-case (first (:name selected))))
    )
  )

(def navbar
  [:div {:class "top-bar"}
   [:div {:class "container-fluid"}
    [:div {:class "row d-flex align-items-center"}
     [:div {:class "col-lg-6 hidden-lg-down text-col"}
      [:ul {:class "list-inline"}
       [:li {:class "list-inline-item"}
        [:i {:class "icon-telephone"}]"020-800-456-747"]
       [:li {:class "list-inline-item"} "Free shipping on orders over $300"]]]
     [:div {:class "col-lg-6 d-flex justify-content-end"}  ;<!-- Language Dropdown-->
      [:div {:class "dropdown show"}
       [:a {:id "langsDropdown", :href "https://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
        [:img {:src "img/united-kingdom.svg", :alt "english"}]"English"]
       [:div {:aria-labelledby "langsDropdown", :class "dropdown-menu"}
        [:a {:href "#", :class "dropdown-item"}
         [:img {:src "img/germany.svg", :alt "german"}]"German"]
        [:a {:href "#", :class "dropdown-item"}
         [:img {:src "img/france.svg", :alt "french"}]"French"]]] ;"<!-- Currency Dropdown-->"
      [:div {:class "dropdown show"}
       [:a {:id "currencyDropdown", :href "#", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"} "USD"]
       [:div {:aria-labelledby "currencyDropdown", :class "dropdown-menu"}
        [:a {:href "#", :class "dropdown-item"} "EUR"]
        [:a {:href "#", :class "dropdown-item"} "GBP"]]]]]]])

(defn sites-row[app site]
  ^{:key (gensym "cs")}
  [:a {:href "#", :class "dropdown-item" :on-click (fn[e] (. e preventDefault)(change-site (:id site) app))} (:name site)]
  )

(defn navbar'[app]
  [:div {:class "top-bar"}
   [:div {:class "container-fluid"}
    [:div {:class "row d-flex align-items-center"}
     [:div {:class "col-lg-6 hidden-lg-down text-col"}
      [:ul {:class "list-inline"}
       [:li {:class "list-inline-item"}
        [:i {:class "icon-telephone"}]"020-800-456-747"]
       [:li {:class "list-inline-item"} "Free shipping on orders over $300"]]]
     [:div {:class "col-lg-6 d-flex justify-content-end"}  ;<!-- Language Dropdown-->
      [:div {:class "dropdown show"}
       [:a {:id "langsDropdown", :href "https://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
        [:img {:src "img/united-kingdom.svg", :alt "english"}]"English"]
       [:div {:aria-labelledby "langsDropdown", :class "dropdown-menu"}
        [:a {:href "#", :class "dropdown-item"}
         [:img {:src "img/germany.svg", :alt "german"}]"German"]
        [:a {:href "#", :class "dropdown-item"}
         [:img {:src "img/france.svg", :alt "french"}]"French"]]] ;"<!-- Currency Dropdown-->"
      [:div {:class "dropdown show"}
       [:a {:id "currencyDropdown", :href "#", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"} "USD"]
       [:div {:aria-labelledby "currencyDropdown", :class "dropdown-menu"}
        [:a {:href "#", :class "dropdown-item"} "EUR"]
        [:a {:href "#", :class "dropdown-item"} "GBP"]]]
      [:div {:class "dropdown show"}
       [:a {:id "sitedropdown", :href "#", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"} (:site-abbrev @app)] ;<--current site inside here...
       [:div {:aria-labelledby "currencyDropdown", :class "dropdown-menu"}
        (let [row-fn (partial sites-row app)]
          (map row-fn (:sites @app))
          )
        ]]]]]])
;(count (:cart @app-state))
;        [:a {:href "#", :class "dropdown-item" :on-click #(fn [e] ((.preventDefault e) (change-site "s14839")))} "Groceries"]]]]]]])

(defn calc-cart-item-total[app]
  (reduce + (map (fn[item] (* (:price item) (:quantity item))) (vals (:cart @app))))
  )

(defn currency[val]
  (fmt/currency-format val)
  )

;Date format example for the future

;(defn date-format [date fmt & [tz]]
;  (let [formatter (goog.i18n.DateTimeFormat. fmt)]
;    (if tz
;      (.format formatter date tz)
;      (.format formatter date))))

(declare delete-cart-item)

(defn mini-cart-row[app cart-item]
  (let [{:keys [:thumbnail :price :name :quantity :key :description :sku]} cart-item]
    ^{:key (str "mcr-" key)}
[:div {:class "dropdown-item cart-product"}
 [:div {:class "d-flex align-items-center"}
  [:div {:class "img"}
   [:img {:src thumbnail, :alt "...", :class "img-fluid"}]]
  [:div {:class "details d-flex justify-content-between"}
   [:div {:class "text"}
    [:a {:href "#"}
     [:strong name]]
    [:small (str "Quantity: " quantity)]
    [:span {:class "price"} (currency price)]]
   [:div {:class "delete"}
    [:i {:class "fa fa-trash-o" :on-click #(delete-cart-item key app)}]]]]]

    ))

(defn menu'[app]

  [:nav {:class "navbar navbar-expand-lg"}
   [:div {:class "search-area"}
    [:div {:class "search-area-inner d-flex align-items-center justify-content-center"}
     [:div {:class "close-btn"}
      [:i {:class "icon-close"}]]
     [:form {:action "#"}
      [:div {:class "form-group"}
       [:input {:type "search", :name "search", :id "search", :placeholder "What are you looking for?"}]
       [:button {:type "submit", :class "submit"}
        [:i {:class "icon-search"}]]]]]]
   [:div {:class "container-fluid"}  ; "<!-- Navbar Header  -->"
    [:a {:href "index.html", :class "navbar-brand"}
     [:img {:src "https://www.fluentcommerce.com/wp-content/uploads/2017/11/logo.svg", :alt "F"}]] ;img/logo.png
    [:button {:type "button", :data-toggle "collapse", :data-target "#navbarCollapse", :aria-controls "navbarCollapse", :aria-expanded "false", :aria-label "Toggle navigation", :class "navbar-toggler navbar-toggler-right"}
     [:i {:class "fa fa-bars"}]] ;"<!-- Navbar Collapse -->"
    [:div {:id "navbarCollapse", :class "collapse navbar-collapse"}
     [:ul {:class "navbar-nav mx-auto"}
      [:li {:class "nav-item"}
       [:a {:href "#", :class "nav-link active"} "Home"]]

      ;[:li {:class "nav-item"}
      ; [:a {:href "category.html", :class "nav-link"} "Shop"]] ;"<!-- Megamenu-->"
      ;
      ;[:li {:class "nav-item dropdown menu-large"}
      ; [:a {:href "#", :data-toggle "dropdown", :class "nav-link"} "Template"
      ;  [:i {:class "fa fa-angle-down"}]]
      ; [:div {:class "dropdown-menu megamenu"}
      ;  [:div {:class "row"}
      ;   [:div {:class "col-lg-9"}
      ;    [:div {:class "row"}
      ;     [:div {:class "col-lg-3"}
      ;      [:strong {:class "text-uppercase"} "Home"]
      ;      [:ul {:class "list-unstyled"}
      ;       [:li
      ;        [:a {:href "index.html"} "Homepage 1"]]]
      ;      [:strong {:class "text-uppercase"} "Shop"]
      ;      [:ul {:class "list-unstyled"}
      ;       [:li
      ;        [:a {:href "#categories"} "Category - left sidebar"]]
      ;       [:li
      ;        [:a {:href "category-right.html"} "Category - right sidebar"]]
      ;       [:li
      ;        [:a {:href "category-full.html"} "Category - full width"]]
      ;       [:li
      ;        [:a {:href "#hello"} "Product detail"]]]] ;detail.html
      ;     [:div {:class "col-lg-3"}
      ;      [:strong {:class "text-uppercase"} "Order process"]
      ;      [:ul {:class "list-unstyled"}
      ;       [:li
      ;        [:a {:href "#cart"} "Shopping cart"]]
      ;       [:li
      ;        [:a {:href "#checkout-address"} "Checkout 1 - Address"]]
      ;       [:li
      ;        [:a {:href "#checkout-shipping"} "Checkout 2 - Delivery"]]
      ;       [:li
      ;        [:a {:href "#checkout-payment"} "Checkout 3 - Payment"]]
      ;       [:li
      ;        [:a {:href "#checkout-summary"} "Checkout 4 - Confirmation"]]]
      ;      [:strong {:class "text-uppercase"} "Blog"]
      ;      [:ul {:class "list-unstyled"}
      ;       [:li
      ;        [:a {:href "blog.html"} "Blog"]]
      ;       [:li
      ;        [:a {:href "post.html"} "Post"]]]]
      ;     [:div {:class "col-lg-3"}
      ;      [:strong {:class "text-uppercase"} "Pages"]
      ;      [:ul {:class "list-unstyled"}
      ;       [:li
      ;        [:a {:href "contact.html"} "Contact"]]
      ;       [:li
      ;        [:a {:href "about.html"} "About us"]]
      ;       [:li
      ;        [:a {:href "text.html"} "Text page"]]
      ;       [:li
      ;        [:a {:href "404.html"} "Error 404"]]
      ;       [:li
      ;        [:a {:href "500.html"} "Error 500"]]
      ;       [:li "More coming soon"]]]
      ;     [:div {:class "col-lg-3"}
      ;      [:strong {:class "text-uppercase"} "Some more content"]
      ;      [:ul {:class "list-unstyled"}
      ;       [:li
      ;        [:a {:href "#"} "Demo content"]]
      ;       [:li
      ;        [:a {:href "#"} "Demo content"]]
      ;       [:li
      ;        [:a {:href "#"} "Demo content"]]
      ;       [:li
      ;        [:a {:href "#"} "Demo content"]]
      ;       [:li
      ;        [:a {:href "#"} "Demo content"]]
      ;       [:li
      ;        [:a {:href "#"} "Demo content"]]
      ;       [:li
      ;        [:a {:href "#"} "Demo content"]]
      ;       [:li
      ;        [:a {:href "#"} "Demo content"]]]]]
      ;    [:div {:class "row services-block"}
      ;     [:div {:class "col-xl-3 col-lg-6 d-flex"}
      ;      [:div {:class "item d-flex align-items-center"}
      ;       [:div {:class "icon"}
      ;        [:i {:class "icon-truck text-primary"}]]
      ;       [:div {:class "text"}
      ;        [:span {:class "text-uppercase"} "Free shipping &amp; return"]
      ;        [:small "Free Shipping over $300"]]]]
      ;     [:div {:class "col-xl-3 col-lg-6 d-flex"}
      ;      [:div {:class "item d-flex align-items-center"}
      ;       [:div {:class "icon"}
      ;        [:i {:class "icon-coin text-primary"}]]
      ;       [:div {:class "text"}
      ;        [:span {:class "text-uppercase"} "Money back guarantee"]
      ;        [:small "30 Days Money Back"]]]]
      ;     [:div {:class "col-xl-3 col-lg-6 d-flex"}
      ;      [:div {:class "item d-flex align-items-center"}
      ;       [:div {:class "icon"}
      ;        [:i {:class "icon-headphones text-primary"}]]
      ;       [:div {:class "text"}
      ;        [:span {:class "text-uppercase"} "020-800-456-747"]
      ;        [:small "24/7 Available Support"]]]]
      ;     [:div {:class "col-xl-3 col-lg-6 d-flex"}
      ;      [:div {:class "item d-flex align-items-center"}
      ;       [:div {:class "icon"}
      ;        [:i {:class "icon-secure-shield text-primary"}]]
      ;       [:div {:class "text"}
      ;        [:span {:class "text-uppercase"} "Secure Payment"]
      ;        [:small "Secure Payment"]]]]]]
      ;   [:div {:class "col-lg-3 text-center product-col hidden-lg-down"}
      ;    [:a {:href "detail.html", :class "product-image"}
      ;     [:img {:src "img/shirt.png", :alt "...", :class "img-fluid"}]]
      ;    [:h6 {:class "text-uppercase product-heading"}
      ;     [:a {:href "detail.html"} "Lose Oversized Shirt"]]
      ;    [:ul {:class "rate list-inline"}
      ;     [:li {:class "list-inline-item"}
      ;      [:i {:class "fa fa-star-o text-primary"}]]
      ;     [:li {:class "list-inline-item"}
      ;      [:i {:class "fa fa-star-o text-primary"}]]
      ;     [:li {:class "list-inline-item"}
      ;      [:i {:class "fa fa-star-o text-primary"}]]
      ;     [:li {:class "list-inline-item"}
      ;      [:i {:class "fa fa-star-o text-primary"}]]
      ;     [:li {:class "list-inline-item"}
      ;      [:i {:class "fa fa-star-o text-primary"}]]]
      ;    [:strong {:class "price text-primary"} "$65.00"]
      ;    [:a {:href "#", :class "btn btn-template wide"} "Add to cart"]]]]]

      ;"<!-- /Megamenu end-->"
      ; ;"<!-- Multi level dropdown    -->"
      ;TODO generalize this for multi-level drop downs - quick hack to do single level menu
      (for [category (ds/find-category-by-parent nil)]
        ^{:key (str (gensym "tn-"))}
        [:li {:class "nav-item dropdown"}
         [:a {:id            "navbarDropdownMenuLink", :href "http://example.com", :data-toggle "dropdown",
              :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} (:name category)
          [:i {:class "fa fa-angle-down"}]]

         [:ul {:aria-labelledby "navbarDropdownMenuLink", :class "dropdown-menu"}

          (for [child (concat (ds/find-category-by-parent (:ref category))
                              [{:id (:id category) :name (str "Shop all " (:name category))}])]
            ^{:key (str (gensym "cn-"))}
            [:li
             [:a {:href (str "#categories/" (:id child)), :class "dropdown-item"} (:name child)]]


            ;TODO use this template to create a diffrent [:li structure if it's a nested category
            ;[:li {:class "dropdown-submenu"}
            ; [:a {:id "navbarDropdownMenuLink2", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "Dropdown link"
            ;  [:i {:class "fa fa-angle-down"}]]
            ; [:ul {:aria-labelledby "navbarDropdownMenuLink2", :class "dropdown-menu"}
            ;  [:li
            ;   [:a {:href "#", :class "dropdown-item"} "Action"]]
            ;  [:li {:class "dropdown-submenu"}
            ;   [:a {:id "navbarDropdownMenuLink3", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "Another action"
            ;    [:i {:class "fa fa-angle-down"}]]
            ;   [:ul {:aria-labelledby "navbarDropdownMenuLink3", :class "dropdown-menu"}
            ;    [:li
            ;     [:a {:href "#", :class "dropdown-item"} "Action"]]
            ;    ]]
            ;]]

            ;[:li
            ; [:a {:href "#categories/c-003", :class "dropdown-item"} "Earrings"]]
            ;[:li
            ; [:a {:href "#categories/c-004", :class "dropdown-item"} "Bracelets"]]
            ;[:li
            ; [:a {:href "#categories/c-001", :class "dropdown-item"} "Shop All Jewelery"]
            ; ]
            )

          ;[:li {:class "dropdown-submenu"}
          ; [:a {:id "navbarDropdownMenuLink2", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "Dropdown link"
          ;  [:i {:class "fa fa-angle-down"}]]
          ; [:ul {:aria-labelledby "navbarDropdownMenuLink2", :class "dropdown-menu"}
          ;  [:li
          ;   [:a {:href "#", :class "dropdown-item"} "Action"]]
          ;  [:li {:class "dropdown-submenu"}
          ;   [:a {:id "navbarDropdownMenuLink3", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "Another action"
          ;    [:i {:class "fa fa-angle-down"}]]
          ;   [:ul {:aria-labelledby "navbarDropdownMenuLink3", :class "dropdown-menu"}
          ;    [:li
          ;     [:a {:href "#", :class "dropdown-item"} "Action"]]
          ;    [:li
          ;     [:a {:href "#", :class "dropdown-item"} "Action"]]
          ;    [:li
          ;     [:a {:href "#", :class "dropdown-item"} "Action"]]
          ;    [:li
          ;     [:a {:href "#", :class "dropdown-item"} "Action"]]]]
          ;  [:li
          ;   [:a {:href "#", :class "dropdown-item"} "Something else here"]]]]

          ]
         ] ;"<!-- Multi level dropdown end-->"

        )
      ;[:li {:class "nav-item dropdown"}
      ; [:a {:id "navbarDropdownMenuLink", :href "http://example.com",
      ;      :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false",
      ;      :class "nav-link"} "Apparel"
      ;  [:i {:class "fa fa-angle-down"}]]
      ; [:ul {:aria-labelledby "navbarDropdownMenuLink", :class "dropdown-menu"}
      ;  [:li ]
      ;  [:li
      ;   [:a {:href "#categories/c-011", :class "dropdown-item"} "Shirts"]]
      ;  [:li
      ;   [:a {:href "#categories/c-010", :class "dropdown-item"} "Shop All Apparel"]]
      ;  ]]

      ;[:li {:class "nav-item"}
      ; [:a {:href "blog.html", :class "nav-link"} "Blog "]]
      ;[:li {:class "nav-item"}
      ; [:a {:href "contact.html", :class "nav-link"} "Contact"]]
      ]
     [:div {:class "right-col d-flex align-items-lg-center flex-column flex-lg-row"}  ;"<!-- Search Button-->"
      [:div {:class "search"}
       [:i {:class "icon-search"}]] ;"<!-- User Dropdown-->"
      [:div {:class "user dropdown show"}
       [:a {:id "userdetails", :href "https://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
        [:i {:class "icon-profile"}]]
       [:ul {:aria-labelledby "userdetails", :class "dropdown-menu"}
        [:li {:class "dropdown-item"}
         [:a {:href "#"} "Profile       "]]
        [:li {:class "dropdown-item"}
         [:a {:href "#"} "Orders       "]]
        [:li {:class "dropdown-divider"} "     "]
        [:li {:class "dropdown-item"}
         [:a {:href "#"} "Logout       "]]]] ;"<!-- Cart Dropdown-->"
      [:div {:class "cart dropdown show"}
       [:a {:id "cartdetails", :href "#cart", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
        [:i {:class "icon-cart"}]
        [:div {:class "cart-no"} (reduce + (map :quantity (vals (:cart @app))))]]
       [:a {:href "#cart", :class "text-primary view-cart"} "View Cart"]
       [:div {:aria-labelledby "cartdetails", :class "dropdown-menu"}  ;"<!-- cart item-->"
[:div {:style {:overflow "auto" :height "280px"}}
        (let [make-row (partial mini-cart-row app)]
          (map make-row (vals (:cart @app)))
          )
]
        [:div {:class "dropdown-item total-price d-flex justify-content-between"}
         [:span "Total"]
         [:strong {:class "text-primary"} (currency (calc-cart-item-total app))]] ;"<!-- call to actions-->"

        [:div {:class "dropdown-item CTA d-flex"}
         [:a {:href "#cart", :class "btn btn-template wide"} "View Cart"]
         [:a {:href "#checkout-delivery", :class "btn btn-template wide"} "Checkout"]]]]]]]])
(def menu
  [:nav {:class "navbar navbar-expand-lg"}
   [:div {:class "search-area"}
    [:div {:class "search-area-inner d-flex align-items-center justify-content-center"}
     [:div {:class "close-btn"}
      [:i {:class "icon-close"}]]
     [:form {:action "#"}
      [:div {:class "form-group"}
       [:input {:type "search", :name "search", :id "search", :placeholder "What are you looking for?"}]
       [:button {:type "submit", :class "submit"}
        [:i {:class "icon-search"}]]]]]]
   [:div {:class "container-fluid"}  ; "<!-- Navbar Header  -->"
    [:a {:href "index.html", :class "navbar-brand"}
     [:img {:src "https://fluentcommerce.com/assets/images/fluentcommerce.svg", :alt "..."}]] ;img/logo.png
    [:button {:type "button", :data-toggle "collapse", :data-target "#navbarCollapse", :aria-controls "navbarCollapse", :aria-expanded "false", :aria-label "Toggle navigation", :class "navbar-toggler navbar-toggler-right"}
     [:i {:class "fa fa-bars"}]] ;"<!-- Navbar Collapse -->"
    [:div {:id "navbarCollapse", :class "collapse navbar-collapse"}
     [:ul {:class "navbar-nav mx-auto"}
      [:li {:class "nav-item"}
       [:a {:href "index.html", :class "nav-link active"} "Home"]]
      [:li {:class "nav-item"}
       [:a {:href "category.html", :class "nav-link"} "Shop"]] ;"<!-- Megamenu-->"
      [:li {:class "nav-item dropdown menu-large"}
       [:a {:href "#", :data-toggle "dropdown", :class "nav-link"} "Template"
        [:i {:class "fa fa-angle-down"}]]
       [:div {:class "dropdown-menu megamenu"}
        [:div {:class "row"}
         [:div {:class "col-lg-9"}
          [:div {:class "row"}
           [:div {:class "col-lg-3"}
            [:strong {:class "text-uppercase"} "Home"]
            [:ul {:class "list-unstyled"}
             [:li
              [:a {:href "index.html"} "Homepage 1"]]]
            [:strong {:class "text-uppercase"} "Shop"]
            [:ul {:class "list-unstyled"}
             [:li
              [:a {:href "#categories"} "Category - left sidebar"]]
             [:li
              [:a {:href "category-right.html"} "Category - right sidebar"]]
             [:li
              [:a {:href "category-full.html"} "Category - full width"]]
             [:li
              [:a {:href "#hello"} "Product detail"]]]] ;detail.html
           [:div {:class "col-lg-3"}
            [:strong {:class "text-uppercase"} "Order process"]
            [:ul {:class "list-unstyled"}
             [:li
              [:a {:href "#cart"} "Shopping cart"]]
             [:li
              [:a {:href "#checkout-address"} "Checkout 1 - Address"]]
             [:li
              [:a {:href "#checkout-shipping"} "Checkout 2 - Delivery"]]
             [:li
              [:a {:href "#checkout-payment"} "Checkout 3 - Payment"]]
             [:li
              [:a {:href "#checkout-summary"} "Checkout 4 - Confirmation"]]]
            [:strong {:class "text-uppercase"} "Blog"]
            [:ul {:class "list-unstyled"}
             [:li
              [:a {:href "blog.html"} "Blog"]]
             [:li
              [:a {:href "post.html"} "Post"]]]]
           [:div {:class "col-lg-3"}
            [:strong {:class "text-uppercase"} "Pages"]
            [:ul {:class "list-unstyled"}
             [:li
              [:a {:href "contact.html"} "Contact"]]
             [:li
              [:a {:href "about.html"} "About us"]]
             [:li
              [:a {:href "text.html"} "Text page"]]
             [:li
              [:a {:href "404.html"} "Error 404"]]
             [:li
              [:a {:href "500.html"} "Error 500"]]
             [:li "More coming soon"]]]
           [:div {:class "col-lg-3"}
            [:strong {:class "text-uppercase"} "Some more content"]
            [:ul {:class "list-unstyled"}
             [:li
              [:a {:href "#"} "Demo content"]]
             [:li
              [:a {:href "#"} "Demo content"]]
             [:li
              [:a {:href "#"} "Demo content"]]
             [:li
              [:a {:href "#"} "Demo content"]]
             [:li
              [:a {:href "#"} "Demo content"]]
             [:li
              [:a {:href "#"} "Demo content"]]
             [:li
              [:a {:href "#"} "Demo content"]]
             [:li
              [:a {:href "#"} "Demo content"]]]]]
          [:div {:class "row services-block"}
           [:div {:class "col-xl-3 col-lg-6 d-flex"}
            [:div {:class "item d-flex align-items-center"}
             [:div {:class "icon"}
              [:i {:class "icon-truck text-primary"}]]
             [:div {:class "text"}
              [:span {:class "text-uppercase"} "Free shipping &amp; return"]
              [:small "Free Shipping over $300"]]]]
           [:div {:class "col-xl-3 col-lg-6 d-flex"}
            [:div {:class "item d-flex align-items-center"}
             [:div {:class "icon"}
              [:i {:class "icon-coin text-primary"}]]
             [:div {:class "text"}
              [:span {:class "text-uppercase"} "Money back guarantee"]
              [:small "30 Days Money Back"]]]]
           [:div {:class "col-xl-3 col-lg-6 d-flex"}
            [:div {:class "item d-flex align-items-center"}
             [:div {:class "icon"}
              [:i {:class "icon-headphones text-primary"}]]
             [:div {:class "text"}
              [:span {:class "text-uppercase"} "020-800-456-747"]
              [:small "24/7 Available Support"]]]]
           [:div {:class "col-xl-3 col-lg-6 d-flex"}
            [:div {:class "item d-flex align-items-center"}
             [:div {:class "icon"}
              [:i {:class "icon-secure-shield text-primary"}]]
             [:div {:class "text"}
              [:span {:class "text-uppercase"} "Secure Payment"]
              [:small "Secure Payment"]]]]]]
         [:div {:class "col-lg-3 text-center product-col hidden-lg-down"}
          [:a {:href "detail.html", :class "product-image"}
           [:img {:src "img/shirt.png", :alt "...", :class "img-fluid"}]]
          [:h6 {:class "text-uppercase product-heading"}
           [:a {:href "detail.html"} "Lose Oversized Shirt"]]
          [:ul {:class "rate list-inline"}
           [:li {:class "list-inline-item"}
            [:i {:class "fa fa-star-o text-primary"}]]
           [:li {:class "list-inline-item"}
            [:i {:class "fa fa-star-o text-primary"}]]
           [:li {:class "list-inline-item"}
            [:i {:class "fa fa-star-o text-primary"}]]
           [:li {:class "list-inline-item"}
            [:i {:class "fa fa-star-o text-primary"}]]
           [:li {:class "list-inline-item"}
            [:i {:class "fa fa-star-o text-primary"}]]]
          [:strong {:class "price text-primary"} "$65.00"]
          [:a {:href "#", :class "btn btn-template wide"} "Add to cart"]]]]] ;"<!-- /Megamenu end-->"
      ; ;"<!-- Multi level dropdown    -->"
      [:li {:class "nav-item dropdown"}
       [:a {:id "navbarDropdownMenuLink", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "Dropdown"
        [:i {:class "fa fa-angle-down"}]]
       [:ul {:aria-labelledby "navbarDropdownMenuLink", :class "dropdown-menu"}
        [:li
         [:a {:href "#", :class "dropdown-item"} "Action"]]
        [:li
         [:a {:href "#", :class "dropdown-item"} "Another action"]]
        [:li {:class "dropdown-submenu"}
         [:a {:id "navbarDropdownMenuLink2", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "Dropdown link"
          [:i {:class "fa fa-angle-down"}]]
         [:ul {:aria-labelledby "navbarDropdownMenuLink2", :class "dropdown-menu"}
          [:li
           [:a {:href "#", :class "dropdown-item"} "Action"]]
          [:li {:class "dropdown-submenu"}
           [:a {:id "navbarDropdownMenuLink3", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "\n                          Another action"
            [:i {:class "fa fa-angle-down"}]]
           [:ul {:aria-labelledby "navbarDropdownMenuLink3", :class "dropdown-menu"}
            [:li
             [:a {:href "#", :class "dropdown-item"} "Action"]]
            [:li
             [:a {:href "#", :class "dropdown-item"} "Action"]]
            [:li
             [:a {:href "#", :class "dropdown-item"} "Action"]]
            [:li
             [:a {:href "#", :class "dropdown-item"} "Action"]]]]
          [:li
           [:a {:href "#", :class "dropdown-item"} "Something else here"]]]]]] ;"<!-- Multi level dropdown end-->"
      [:li {:class "nav-item"}
       [:a {:href "blog.html", :class "nav-link"} "Blog "]]
      [:li {:class "nav-item"}
       [:a {:href "contact.html", :class "nav-link"} "Contact"]]]
     [:div {:class "right-col d-flex align-items-lg-center flex-column flex-lg-row"}  ;"<!-- Search Button-->"
      [:div {:class "search"}
       [:i {:class "icon-search"}]] ;"<!-- User Dropdown-->"
      [:div {:class "user dropdown show"}
       [:a {:id "userdetails", :href "https://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
        [:i {:class "icon-profile"}]]
       [:ul {:aria-labelledby "userdetails", :class "dropdown-menu"}
        [:li {:class "dropdown-item"}
         [:a {:href "#"} "Profile       "]]
        [:li {:class "dropdown-item"}
         [:a {:href "#"} "Orders       "]]
        [:li {:class "dropdown-divider"} "     "]
        [:li {:class "dropdown-item"}
         [:a {:href "#"} "Logout       "]]]] ;"<!-- Cart Dropdown-->"
      [:div {:class "cart dropdown show"}
       [:a {:id "cartdetails", :href "#cart", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
        [:i {:class "icon-cart"}]
        [:div {:class "cart-no"} "1"]]
       [:a {:href "#cart", :class "text-primary view-cart"} "View Cart"]
       [:div {:aria-labelledby "cartdetails", :class "dropdown-menu"}  ;"<!-- cart item-->"
        [:div {:class "dropdown-item cart-product"}
         [:div {:class "d-flex align-items-center"}
          [:div {:class "img"}
           [:img {:src "img/hoodie-man-1.png", :alt "...", :class "img-fluid"}]]
          [:div {:class "details d-flex justify-content-between"}
           [:div {:class "text"}
            [:a {:href "#"}
             [:strong "Heather Gray Hoodie"]]
            [:small "Quantity: 1 "]
            [:span {:class "price"} "$75.00 "]]
           [:div {:class "delete"}
            [:i {:class "fa fa-trash-o"}]]]]] ;"<!-- total price-->"
        [:div {:class "dropdown-item total-price d-flex justify-content-between"}
         [:span "Total"]
         [:strong {:class "text-primary"} "$75.00"]] ;"<!-- call to actions-->"
        [:div {:class "dropdown-item CTA d-flex"}
         [:a {:href "#cart", :class "btn btn-template wide"} "View Cart"]
         [:a {:href "#checkout-address", :class "btn btn-template wide"} "Checkout"]]]]]]]])

(def hero-items
  [:section {:class "hero hero-home no-padding"}
   [:div {:class "owl-carousel owl-theme hero-slider"}
    [:div {:style {:background "url(img/hero-bg.jpg)"} :class "item d-flex align-items-center has-pattern"}
     [:div {:class "container"}
      [:div {:class "row"}
       [:div {:class "col-lg-6"}
        [:h1 "The Hub"]
        [:ul {:class "lead"}
         [:li
          [:strong "Bootstrap 4 E-commerce"]" template"]
         [:li
          [:strong "18"]" pages, "
          [:strong "6"]" colour variants"]
         [:li
          [:strong "SCSS"]" sources "]
         [:li "frequent &amp; "
          [:strong "free updates"]]]
        [:a {:href "#", :class "btn btn-template wide shop-now"} "Shop Now"
         [:i {:class "icon-bag"} " "]]]]]]
    [:div {:style {:background "url(img/hero-bg-2.jpg)"} :class "item d-flex align-items-center"}
     [:div {:class "container"}
      [:div {:class "row"}
       [:div {:class "col-lg-6 text-white"}
        [:h1 "Labore et dolore magna aliqua"]
        [:p {:class "lead"} "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."]
        [:a {:href "#", :class "btn btn-template wide shop-now"} "Shop Now"
         [:i {:class "icon-bag"} "  "]]]]]]
    [:div {:style {:background "url(img/hero-bg-3.jpg)"} :class "item d-flex align-items-center"}
     [:div {:class "container"}
      [:div {:class "row"}
       [:div {:class "col-lg-6 text-white"}
        [:h1 "Sed do eiusmod tempor"]
        [:p {:class "lead"} "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."]
        [:a {:href "#", :class "btn btn-template wide shop-now"} "Shop Now"
         [:i {:class "icon-bag"} "           "]]]]]]]])

(defn product-category-swatch [index]
  [:div {:class "col-lg-4"}
   [:a {:href "#"}
    [:div {:style {:background-image (str "url(img/banner-" index ".jpg)")} :class "item d-flex align-items-end"}
     [:div {:class "content"}
      [:h3 {:class "h5"} "Men's"]
      [:span "New On Sale Collection"]]]]]
  )

(def categories
  [:section {:class "categories"}
   [:div {:class "container"}
    [:header {:class "text-center"}
     [:h2 {:class "text-uppercase"}
      [:small "Top for this month"]"Our Featured Picks"]]
    [:div {:class "row text-left"}
     (map product-category-swatch (range 1 4))]]])

;item list in category page

;[:div {:class "item col-xl-4 col-md-6"}
; [:div {:class "product is-gray"}
;  [:div {:class "image d-flex align-items-center justify-content-center"}
;   [:div {:class "ribbon ribbon-primary text-uppercase"} "Sale"]
;   [:img {:src "img/hoodie-man-1.png", :alt "product", :class "img-fluid"}]
;   [:div {:class "hover-overlay d-flex align-items-center justify-content-center"}
;    [:div {:class "CTA d-flex align-items-center justify-content-center"}
;     [:a {:href "#", :class "add-to-cart"}
;      [:i {:class "fa fa-shopping-cart"}]]
;     [:a {:href "detail.html", :class "visit-product active"}
;      [:i {:class "icon-search"}]"View"]
;     [:a {:href "#", :data-toggle "modal", :data-target "#exampleModal", :class "quick-view"}
;      [:i {:class "fa fa-arrows-alt"}]]]]]
;  [:div {:class "title"}
;   [:small {:class "text-muted"} "Men Wear"]
;   [:a {:href "detail.html"}
;    [:h3 {:class "h6 text-uppercase no-margin-bottom"} "Elegant Lake"]]
;   [:span {:class "price text-muted"} "$40.00"]]]]

(defn item-swatch[gender, index]
  [:div {:class "item"}
   [:div {:class "product is-gray"}
    [:div {:class "image d-flex align-items-center justify-content-center"}
     [:img {:src (str "img/hoodie-" gender "-" index ".png"), :alt "product", :class "img-fluid"}]
     [:div {:class "hover-overlay d-flex align-items-center justify-content-center"}
      [:div {:class "CTA d-flex align-items-center justify-content-center"}
       [:a {:href "#", :class "add-to-cart"}
        [:i {:class "fa fa-shopping-cart"}]]
       [:a {:href "detail.html", :class "visit-product active"}
        [:i {:class "icon-search"}]"View"]
       [:a {:href "#", :data-toggle "modal", :data-target "#exampleModal", :class "quick-view"}
        [:i {:class "fa fa-arrows-alt"}]]]]]
    [:div {:class "title"}
     [:a {:href "detail.html"}
      [:h3 {:class "h6 text-uppercase no-margin-bottom"} "Stylish hoodie"]]
     [:span {:class "price text-muted"} "$40.00"]]]]
  )

(def products-men
  [:section {:class "men-collection gray-bg"}
   [:div {:class "container"}
    [:header {:class "text-center"}
     [:h2 {:class "text-uppercase"}
      [:small "Autumn Choice"]"Men Collection"]] ;"<!-- Products Slider-->"
    [:div {:class "owl-carousel owl-theme products-slider"}  ;"<!-- item-->"

     (map (partial item-swatch "man") (take 6 (cycle [1 2 3 4])))

     ]]])

(def divider
  [:section {:style {:background "url(img/divider-bg.jpg)"} :class "divider"}
   [:div {:class "container"}
    [:div {:class "row"}
     [:div {:class "col-lg-6"}
      [:p "Old Collection                  "]
      [:h2 {:class "h1 text-uppercase no-margin"} "Huge Sales"]
      [:p "At our outlet stores"]
      [:a {:href "#", :class "btn btn-template wide shop-now"} "Shop Now"
       [:i {:class "icon-bag"}]]]]]])

(def products-women
  [:section {:class "women-collection"}
   [:div {:class "container"}
    [:header {:class "text-center"}
     [:h2 {:class "text-uppercase"}
      [:small "Ladies' Time"]"Women Collection"]] ;"<!-- Products Slider-->"
    [:div {:class "owl-carousel owl-theme products-slider"}  ;"<!-- item-->"

     (map (partial item-swatch "woman") (take 24 (cycle [1 2 3 4])))

     ]]])

(def brands
  [:section {:class "brands"}
   [:div {:class "container"}
    [:div {:class "owl-carousel owl-theme brands-slider"}
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-1.svg", :alt "...", :class "img-fluid"}]]]
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-2.svg", :alt "...", :class "img-fluid"}]]]
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-3.svg", :alt "...", :class "img-fluid"}]]]
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-4.svg", :alt "...", :class "img-fluid"}]]]
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-5.svg", :alt "...", :class "img-fluid"}]]]
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-6.svg", :alt "...", :class "img-fluid"}]]]
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-1.svg", :alt "...", :class "img-fluid"}]]]
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-2.svg", :alt "...", :class "img-fluid"}]]]
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-3.svg", :alt "...", :class "img-fluid"}]]]
     [:div {:class "item d-flex align-items-center justify-content-center"}
      [:div {:class "brand d-flex align-items-center"}
       [:img {:src "img/brand-4.svg", :alt "...", :class "img-fluid"}]]]]]])

(def example-modal
  [:div {:id "exampleModal", :tabindex "-1", :role "dialog", :aria-hidden "true", :class "modal fade overview"}
   [:div {:role "document", :class "modal-dialog"}
    [:div {:class "modal-content"}
     [:button {:type "button", :data-dismiss "modal", :aria-label "Close", :class "close"}
      [:span {:aria-hidden "true"}
       [:i {:class "icon-close"}]]]]
    [:div {:class "modal-body"}
     [:div {:class "ribbon-primary text-uppercase"} "Sale"]
     [:div {:class "row d-flex align-items-center"}
      [:div {:class "image col-lg-5"}
       [:img {:src "img/shirt.png", :alt "...", :class "img-fluid d-block"}]]
      [:div {:class "details col-lg-7"}
       [:h2 "Loose Oversized Shirt"]
       [:ul {:class "price list-inline"}
        [:li {:class "list-inline-item current"} "$65.00"]
        [:li {:class "list-inline-item original"} "$90.00"]]
       [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco"]
       [:div {:class "d-flex align-items-center"}
        [:div {:class "quantity d-flex align-items-center"}
         [:div {:class "dec-btn"} "-"]
         [:input {:type "text", :value "1", :class "quantity-no"}]
         [:div {:class "inc-btn"} "+"]]
        [:select {:id "size", :class "bs-select"}
         [:option {:value "small"} "Small"]
         [:option {:value "meduim"} "Medium"]
         [:option {:value "large"} "Large"]
         [:option {:value "x-large"} "X-Large"]]]
       [:ul {:class "CTAs list-inline"}
        [:li {:class "list-inline-item"}
         [:a {:href "#", :class "btn btn-template wide"}
          [:i {:class "fa fa-shopping-cart"}]"Add to Cart"]]
        [:li {:class "list-inline-item"}
         [:a {:href "#", :class "visit-product active btn-template-outlined wide"}
          [:i {:class "icon-search"}]"View\n                    Add to wishlist"]]]]]]]])

(def header
  [:header {:class "header"} navbar menu])

(defn header' [app]
  [:header {:class "header"} [navbar' app] [menu' app]]
  )

(defn category-item-row[app product]
  (let [{:keys [:thumbnail :id :parent :name :price :description]} product]

    (println "Rendering: " product)

    ^{:key (str (gensym "cati-"))}
    [:div {:class "item col-xl-4 col-md-6"}
     [:div {:class "product is-gray"}
      [:div {:class "image d-flex align-items-center justify-content-center"}
       ;[:div {:class "ribbon ribbon-primary text-uppercase"} "Sale"] ;put "Sale" tag on if orig < price?
       [:img {:src thumbnail, :alt "product", :class "img-fluid"}]
       [:div {:class "hover-overlay d-flex align-items-center justify-content-center"}
        [:div {:class "CTA d-flex align-items-center justify-content-center"}
         ;[:a {:href (str "#/product-details/" id), :class "add-to-cart"}
         ; [:i {:class "fa fa-shopping-cart"}]]
         [:a {:href (str "#/product-details/" id), :class "visit-product active"}
          [:i {:class "icon-search"}]"View"]
         ;[:a {:href "#", :data-toggle "modal", :data-target "#exampleModal", :class "quick-view"}
         ; [:i {:class "fa fa-arrows-alt"}]]
         ]]]
      [:div {:class "title"}
       ;[:small {:class "text-muted"} "Men Wear"] ;show category name here?
       [:a {:href (str "#/product-details/" id)}
        [:h3 {:class "h6 text-uppercase no-margin-bottom"} name]]
       [:span {:class "price text-muted"} (currency price)]]]]

    ))

(defn product-list [app]

  (println "components :: product-list")

  (let [category-id (get-in @app [:args :category-id])
        products (ds/find-products-by-supercategory category-id)
        ]

    (println (str "view category id : " category-id " and with products count: " (count products)))

    [:div {:class "container"}
     [:div {:class "row"}  ;"<!-- Sidebar-->"
      [:div {:class "sidebar col-xl-3 col-lg-4 sidebar"}
       [:div {:class "block"}
        [:h6 {:class "text-uppercase"} "Product Categories"]
        [:ul {:class "list-unstyled"}
         [:li
          [:a {:href "#", :class "d-flex justify-content-between align-items-center"}
           [:span "Men's Collection"]
           [:small "200"]]
          [:ul {:class "list-unstyled"}
           [:li " "
            [:a {:href "#"} "T-shirts"]]
           [:li " "
            [:a {:href "#"} "Hoodies"]]
           [:li " "
            [:a {:href "#"} "Shorts"]]]]
         [:li {:class "active"}
          [:a {:href "#", :class "d-flex justify-content-between align-items-center"}
           [:span "Women's Collection"]
           [:small "120"]]
          [:ul {:class "list-unstyled"}
           [:li " "
            [:a {:href "#"} "T-shirts"]]
           [:li " "
            [:a {:href "#"} "Dresses"]]
           [:li " "
            [:a {:href "#"} "Pants"]]
           [:li " "
            [:a {:href "#"} "Shorts"]]]]
         [:li
          [:a {:href "#", :class "d-flex justify-content-between align-items-center"}
           [:span "Accessories"]
           [:small "80"]]
          [:ul {:class "list-unstyled"}
           [:li " "
            [:a {:href "#"} "Wallets"]]
           [:li " "
            [:a {:href "#"} "Backpacks"]]
           [:li " "
            [:a {:href "#"} "Belts"]]
           [:li " "
            [:a {:href "#"} "Necklaces"]]]]]]
       [:div {:class "block"}
        [:h6 {:class "text-uppercase"} "Filter By Price  "]
        [:div {:id "slider-snap"}]
        [:div {:class "value d-flex justify-content-between"}
         [:div {:class "min"} "From "
          [:span {:id "slider-snap-value-lower", :class "example-val"}] "$"]
         [:div {:class "max"} "To "
          [:span {:id "slider-snap-value-upper", :class "example-val"}] "$"]]
        [:a {:href "#", :class "filter-submit"} "filter"]]
       [:div {:class "block"}
        [:h6 {:class "text-uppercase"} "Brands "]
        [:form {:action "#"}
         [:div {:class "form-group"}
          [:input {:id "calvin" :type "checkbox" :name "clothes-brand" :checked "checked" :class "checkbox-template"}]
          [:label {:for "calvin"} "Calvin Klein "
           [:small "(18)"]]]
         [:div {:class "form-group"}
          [:input {:id "levi-strauss", :type "checkbox", :name "clothes-brand", :checked true , :class "checkbox-template"}]
          [:label {:for "levi-strauss"} "Levi Strauss "
           [:small "(30)"]]]
         [:div {:class "form-group"}
          [:input {:id "hugo-boss", :type "checkbox", :name "clothes-brand", :class "checkbox-template"}]
          [:label {:for "hugo-boss"} "Hugo Boss "
           [:small "(120)"]]]
         [:div {:class "form-group"}
          [:input {:id "tomi-hilfiger", :type "checkbox", :name "clothes-brand", :class "checkbox-template"}]
          [:label {:for "tomi-hilfiger"} "Tomi Hilfiger "
           [:small "(70)"]]]
         [:div {:class "form-group"}
          [:input {:id "tom-ford", :type "checkbox", :name "clothes-brand", :class "checkbox-template"}]
          [:label {:for "tom-ford"} "Tom Ford "
           [:small "(110)"]]]]]
       [:div {:class "block"}
        [:h6 {:class "text-uppercase"} "Size "]
        [:form {:action "#"}
         [:div {:class "form-group"}
          [:input {:id "small", :type "radio", :name "size", :class "radio-template"}]
          [:label {:for "small"} "Small"]]
         [:div {:class "form-group"}
          [:input {:id "medium", :type "radio", :name "size", :checked "checked" , :class "radio-template"}]
          [:label {:for "medium"} "Medium"]]
         [:div {:class "form-group"}
          [:input {:id "large", :type "radio", :name "size", :class "radio-template"}]
          [:label {:for "large"} "Large"]]
         [:div {:class "form-group"}
          [:input {:id "x-large", :type "radio", :name "size", :class "radio-template"}]
          [:label {:for "x-large"} "x-Large"]]]]] ;"<!-- /Sidebar end-->"  "<!-- Grid -->"
      [:div {:class "products-grid col-xl-9 col-lg-8 sidebar-left"}
       [:header {:class "d-flex justify-content-between align-items-start"}
        [:span {:class "visible-items"} "Showing "
         [:strong "1-15 "] "of "
         [:strong "158 "] "results"]
        [:select {:id "sorting", :class "bs-select"}
         [:option {:value "newest"} "Newest"]
         [:option {:value "oldest"} "Oldest"]
         [:option {:value "lowest-price"} "Low Price"]
         [:option {:value "heigh-price"} "High Price"]]]
       [:div {:class "row"}  ;"<!-- item-->"

        ;category-row
        (let [make-product (partial category-item-row app)]
          (map make-product products)
          )

        ]

       [:nav {:aria-label "page navigation example", :class "d-flex justify-content-center"}
        [:ul {:class "pagination pagination-custom"}
         [:li {:class "page-item"}
          [:a {:href "#", :aria-label "Previous", :class "page-link"}
           [:span {:aria-hidden "true"} "Prev"]
           [:span {:class "sr-only"} "Previous"]]]
         [:li {:class "page-item"}
          [:a {:href "#", :class "page-link active"} "1       "]]
         [:li {:class "page-item"}
          [:a {:href "#", :class "page-link"} "2       "]]
         [:li {:class "page-item"}
          [:a {:href "#", :class "page-link"} "3       "]]
         [:li {:class "page-item"}
          [:a {:href "#", :class "page-link"} "4       "]]
         [:li {:class "page-item"}
          [:a {:href "#", :class "page-link"} "5 "]]
         [:li {:class "page-item"}
          [:a {:href "#", :aria-label "Next", :class "page-link"}
           [:span {:aria-hidden "true"} "Next"]
           [:span {:class "sr-only"} "Next     "]]]]]]]]
    )
  )

(defn section-header[app title breadcrumb]
  [:section {:class "hero hero-page gray-bg padding-small"}
   [:div {:class "container"}
    [:div {:class "row d-flex"}
     [:div {:class "col-lg-9 order-2 order-lg-1"}
      [:h1 title]
      [:p {:class "lead text-muted"} (str "You currently have " (reduce + (map :quantity (vals (:cart @app)))) " items in your shopping cart")]
      [:p {:class "lead text-muted"} (str "Cart total " (currency (calc-cart-item-total app)) )]
      ]
     [:ul {:class "breadcrumb d-flex justify-content-start justify-content-lg-center col-lg-3 text-right order-1 order-lg-2"}
      [:li {:class "breadcrumb-item"}
       [:a {:href "index.html"} "Home"]]
      [:li {:class "breadcrumb-item active"} breadcrumb]]]]])

(defn quotes[val]
  (str "\"" val "\"") )

(defn inc-quantity [key app]
  (let [qty (get-in @app [:cart key]) item (get-in @app [:cart key] )]
    (swap! app update-in [:cart key :quantity] inc)
    )
  )

(defn delete-cart-item [key app]
  (swap! app update-in [:cart] dissoc key)
  )

(defn dec-quantity [key app]
  (let [qty (get-in @app [:cart key :quantity]) item (get-in @app [:cart key]  )]
    (if (<= qty 1)
      (delete-cart-item key app)
      (swap! app update-in [:cart key :quantity] dec)
      )
    )
  )

(defn cart-row[app cart-item]
  (let [{:keys [:thumbnail :parent :name :price :quantity :key :description]} cart-item]
    (.log js/console (str "tn: " thumbnail "cost: " (currency price) "qty: " quantity))
    ^{:key (str "ci-" key)}
    [:div {:class "item"}
     [:div {:class "row d-flex align-items-center"}
      [:div {:class "col-5"}
       [:div {:class "d-flex align-items-center"}
        [:img {:src thumbnail, :alt "...", :class "img-fluid"}]
        [:div {:class "title"}
         [:a {:href (str "#/product-details/" parent)}
          [:h5 name]
          ;[:span {:class "text-muted"} "Size: Large"] ;secondary value if needed
          ]]]]
      [:div {:class "col-2"}
       [:span (currency price)]]
      [:div {:class "col-2"}
       [:div {:class "d-flex align-items-center"}
        [:div {:class "quantity d-flex align-items-center"}
         [:div {:class "dec-btn" :on-click #(dec-quantity key app)} "-"]
         [:input {:type "text", :value quantity, :class "quantity-no"}]
         [:div {:class "inc-btn" :on-click #(inc-quantity key app)} "+"]]]]
      [:div {:class "col-2"}
       [:span (currency (* price quantity))]]
      [:div {:class "col-1 text-center"}
       [:i {:class "delete fa fa-trash" :on-click #(delete-cart-item key app)}]]]]
    )
  )

(defn cart-contents [app]

  ;(.log js/console (str "Cart value in cart-contents:\n " (with-out-str (pp/pprint cart )) ))
  ;
  ;(let [items (map cart-row cart)]
  ;  (.log js/console (str "cart rows:\n " (with-out-str (pp/pprint items )) ))
  ;  (.log js/console (str "cart rows as ved:\n " (with-out-str (pp/pprint (vec items) )) ))
  (let [make-row (partial cart-row app)]
    [:div
     [:section {:class "shopping-cart"}

      [:div {:class "container"}
       [:div {:class "basket"}
        [:div {:class "basket-holder"}
         [:div {:class "basket-header"}
          [:div {:class "row"}
           [:div {:class "col-5"} "Product"]
           [:div {:class "col-2"} "Price"]
           [:div {:class "col-2"} "Quantity"]
           [:div {:class "col-2"} "Total"]
           [:div {:class "col-1 text-center"} "Remove"]]]
         [:div {:class "basket-body"}
          (map make-row (vals (:cart @app)))
          ]]]]
      [:div {:class "container"}
       [:div {:class "CTAs d-flex align-items-center justify-content-center justify-content-md-end flex-column flex-md-row"}
        ;[:a {:href "shop.html", :class "btn btn-template-outlined wide"} "Continue Shopping"]
        [:a {:href "#checkout-delivery", :class "btn btn-template wide"} "Checkout"]]]]
     ]
    )
    )
;)

(defn order-summary [app]
  (let [item-total (calc-cart-item-total app) shipping (:shipping @app)]
    [:div {:class "col-lg-4"}
     [:div {:class "block-body order-summary"}
      [:h6 {:class "text-uppercase"} "Order Summary"]
      [:p "Shipping and additional costs are calculated based on values you have entered"]
      [:ul {:class "order-menu list-unstyled"}
       [:li {:class "d-flex justify-content-between"}
        [:span "Order Subtotal "]
        [:strong (currency item-total)]]
       [:li {:class "d-flex justify-content-between"}
        [:span "Shipping and handling"]
        [:strong (currency shipping)]]
       [:li {:class "d-flex justify-content-between"}
        [:span "Tax"]
        [:strong "$0.00"]]
       [:li {:class "d-flex justify-content-between"}
        [:span "Total"]
        [:strong {:class "text-primary price-total"} (currency (+ item-total shipping))]]]]])
  )

(defn set-value! [id value app doc]
  ;(swap! state assoc :saved? false)
  (swap! app assoc-in [doc id] value))

(defn get-value [id app doc]
  (get-in @app [doc id]))

(defn text-input [id label name placeholder classes app doc]
  [:div {:class (str "form-group " classes)}
   [:label {:for id :class "form-label"} label]
   [:input {:id id :type "text" :name name :placeholder placeholder :class "form-control"
            :value (get-value id app doc) :on-change #(set-value! id (-> % .-target .-value) app doc) }]]
  )

(defn copy-address[app]
  (swap! app assoc-in [:address] (:saved-address @app)
  ))

(defn delivery-method [app method]
  (swap! app assoc :delivery method)
  (when (= method :CC)
    (swap! app assoc :shipping 0.00))
  )

(defn store-cart [app]
  (aset js/window "skus"
        (clj->js (vec (map (fn[item] {"sku" (:sku item) "quantity" (:quantity item)}) (vals (:cart @app))))))
  (delivery-method app :CC)
  )

(defn delivery [app]

  [:section {:class "checkout"}
   [:div {:class "container"}
    [:div {:class "row"}
     [:div {:class "col-md-6"}
      [:div {:class "tab-content"}

       [:div {:id "delivery", :class "active tab-block"}

        [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
         [:a {:href "#cart", :class "btn btn-template-outlined prev"}
          [:i {:class "fa fa-angle-left"}] "Back to Cart"]
         [:a {:href "#checkout-address", :class "btn btn-template " :on-click #(delivery-method app :HD)}
          [:i {:class "fa"}] "Home Delivery"]
         [:a {:href "#checkout-store" :class "btn btn-template " :on-click #(store-cart app)} "Click and Collect"
          [:i {:class "fa"}]]]
        ]]]
     [order-summary app]
     ]]]

  )

(def fulfillment-options-chan (chan))

(defn format-error [response]
  (str "Error returned from server " (:status response) " " (get-in response [:body :message]))
  )

(defn fulfilment-options-event-loop [app-state]
  (go-loop []
           (when-let [response (<! fulfillment-options-chan)]

             (if (= (:status response) 200)
               (do

                 (swap! app-state assoc-in [:fulfillment-options] (:body response))
                 (swap! app-state assoc-in [:fo-spinner] false)

                 )
               (swap! app-state merge @app-state {:fulfillment-options (format-error response)})
               )
             (recur)
             )
           )
  )

(defn check-fulfillment[app]
  (let [cart-items (vec (map (fn[item] {:skuRef (:sku item) :requestedQuantity (:quantity item)}) (vals (:cart @app))))
        selected-store (js->clj (aget js/window "storeAddress" ))]

    (net/log "check-fulfillment::Getting fulfillment options")
    (swap! app assoc :fo-spinner true)
    (net/get-fulfillment-options (get selected-store "StoreId") cart-items 1 fulfillment-options-chan)
    (net/log "check-fulfillment:: fininshed FO call")
    )
  )

(defn store-render [app]
      [:div {:class "container"}
               [:div {:class "CTAs d-flex  flex-column flex-lg-row"}
                [:a {:href "#checkout-delivery", :class "btn btn-template-outlined prev"}
                 [:i {:class "fa fa-angle-left"}] "Back to Delivery"]
                [:span {:style {"padding-left" "50px"}}]
                [:a {:href "#checkout-availability", :class "btn btn-template " :on-click #(check-fulfillment app)} "Check Availability"
                 [:i {:class "fa"}]]
                ;[:span {:style {"padding-left" "50px"}}]
                ;[:a {:href "#checkout-availability", :class "btn btn-template " :on-click #(check-fulfillment app)} "Reserve Items"
                ; [:i {:class "fa"}]]
                ]

        [:div {:class "col-lg-8"}
          [:div {:id "store"}
           ]
         ]
        ]
     )

;TODO fix global reference to skus :(
; perhaps tuck cart contents during previous step as a JS array stored in global state...
(defn store-did-mount [this]
  (println "Store did mount!!")
  ;TODO check for presence of widget and change store vs reinit widget on every component render
  (.initstore js/window (aget js/window "skus"))
  )

(defn store [app]
  (reagent/create-class {:reagent-render      #(store-render app)
                         :component-did-mount store-did-mount}))

;TODO Consider this with-meta approach to hooking into react lifecycle events
;But you can still use them if you want to, either using reagent.core/create-class or by attaching meta-data to a component function:
;
;(defonce my-html (r/atom ""))
;
;(defn plain-component []
;  [:p "My html is " @my-html])
;
;(def component-with-callback
;  (with-meta plain-component
;             {:component-did-mount
;              (fn [this]
;                (reset! my-html (.-innerHTML (reagent/dom-node this))))}))


(defn substitute-item[app key substitute]
  (println (str "Subbing key: " key))
  (let [updated (merge (get-in @app [:cart key]) substitute)]
    (println updated)
    (swap! app assoc-in [:cart key] updated)
    )
  )

(defn cart-availability-row[app cart-item]
  (let [{:keys [:thumbnail :price :quantity :key :name :description :sku]} cart-item
        replacement (:replacement @app)]
    ^{:key (str "cri-" key)}
    [:div {:class "item row d-flex align-items-center"}
     [:div {:class "col-6"}
      [:div {:class "d-flex align-items-center"}
       [:img {:src thumbnail, :alt "...", :class "img-fluid"}]
       [:div {:class "title"}
        [:a {:href "#"} ;show item details...
         [:h6 name]
         [:span {:class "text-muted"} (str "Sku: " sku)]]]]]
     [:div {:class "col-2"}
      [:span (currency price)]]
     [:div {:class "col-2"}
      [:span quantity]]
     [:div {:class "col-2"}
      [:span (currency (* price quantity))]]
     (when (= sku "SKU001")
       [:div {:class "col-6" :style {:paddingLeft "70px"}}[:div {:style {:backgroundColor "#b57983"} :on-click #(delete-cart-item key app)} [:span "Out of Stock - click to remove"]]
       ]
       )
     (when (= sku "SKU703")
       [:div {:class "col-6" :style {:paddingLeft "70px"} :on-click #(substitute-item app key replacement)} [:img {:src (:thumbnail replacement), :alt "..."}]
       [:div { :style {:backgroundColor "#ccbd88"}}
        [:span (str "Item unavailable - click to substitute with: " (:description replacement) "@ " (currency (:price replacement)))]
        ]]
       )
     (when (or (= sku "SKU001") (= sku "SKU703"))
       [:div {:class "delete"}
        [:i {:class "fa fa-trash-o" :on-click #(delete-cart-item key app)}]]
       )
     ]
    ))

(defn variant-select [product-id variant-selector-state-id app selector-id value]
  (let [variant-state (assoc-in (:variant-state @app) [variant-selector-state-id selector-id] value)
        variant-values (set (filter #(not (empty? %)) (vals (get variant-state variant-selector-state-id))))
      ;TODO fix ref to :catalog @app...
        product (ds/find-product-by-id product-id)
        variants (filter #(and (= (:ref product) (:parent %)) (every? (:variant-cats %) variant-values)) @ds/catalog)]
    ;(js/alert (str "variant-select:: variant-state: " variant-state " variant-values: "  ;;(:catalog @app)
    ;              variant-values " valid variants: " variants))
    ;(js/alert (str "vs-id: " variant-selector-state-id " variant-state: " variant-state))
    ;(js/alert (str "app state before: " (:variant-state @app)))
    (swap! app assoc :variant-state variant-state)
    (swap! app assoc-in [:selected-variant product-id] variants)
  ))

(defn make-selects [product-id variant-selector-id variant-selector app]
  (let [
        ordered-sel (sort-by :order variant-selector)
        ]
    (loop [sels ordered-sel selectors [:ul {:class "list-unstyled"}]]
      (if (not (empty? sels))
        (let [current (first sels) tail (rest sels)]
          (if (:selector current)
            (let [[new-select remaining-candidates]
                  (loop [select [:select {:id (:ref current)
                                          :on-change (fn [evt]
                                                       (variant-select product-id variant-selector-id app (:ref current) (-> evt .-target .-value)))
                                          }] values tail]
                    (let [select-val (first values) remaining (rest values)]
                      (if (and (not (:selector select-val)) (not (empty? values)))
                        (recur (conj select [:option {:value (if (:prompt select-val) "" (:ref select-val))} (:name select-val)]) remaining)
                        ;otherwise, we hit a new selector and can return to the outer loop
                          [select values]
                        )
                      )
                    )
                  ]
              (recur remaining-candidates (conj selectors [:li new-select]))
              )
            ;can't do anything with out of order selector... just throw away and continue
            (recur tail selectors)
            )
          )
        [:div selectors]
        ))
    )
  )

(defn add-to-cart[product-id app quantity]
  (let [
        product (ds/find-product-by-id product-id) ;(first (filter #(= product-id (:id %)) (:catalog @app)))
        parent (ds/find-parent-product product-id) ; (first (filter #(= (:parent product) (:id %)) (:catalog @app)))
        cart (get :cart @app {})
        key-val ((:next-key-fn @app))
        cart-keys {:quantity quantity :key key-val}
        ]
    (println (str "add-to-cart:: adding " product-id " to cart"))
    (let [cart-item (if parent
                      (merge parent product cart-keys)
                      (merge product cart-keys)
                      )]
      (println (str "cart item: " cart-item))
      (swap! app assoc-in [:cart key-val] cart-item)
      )
    )
  )

(defn quantity-selector [id]
  (let [selected-quantity (reagent/atom 1)]
    (fn []
      [:div {:class "quantity d-flex align-items-center"}
       [:div {:class "dec-btn" :on-click (fn [e] (when (> @selected-quantity 1) (swap! selected-quantity dec)))} "-"]
       [:input {:type "text", :id id :value @selected-quantity, :class "quantity-no"}]
       [:div {:class "inc-btn" :on-click #(swap! selected-quantity inc)} "+"]]
      )
    )
  )

(defn int-input-value [element-id] (js/parseInt (.-value (.getElementById js/document element-id))))

(defn product-detail [app]

  (let [product-id (get-in @app [:args :product-id])
        base-product (ds/find-product-by-id product-id)   ;(first (filter #(= product-id (:id %)) (:catalog @app)))
        is-variant? (:base base-product)
        valid-variants (get-in @app [:selected-variant product-id])
        variant (merge base-product (first valid-variants))
        variant-selector-id (:variant-selector base-product)
        variant-selector (get (:variant-selector @app) variant-selector-id)
        ]

    (println (str "Pid " product-id " product " base-product " variant-selector "
                  variant-selector " id -> " variant-selector-id))
    (println (str "Variant? " is-variant? " valid count: " (count valid-variants)))

    [:div
     [:section {:class "product-details no-padding-top"}
      [:div {:class "container"}
       [:div {:class "row"}
        [:div {:class "product-images col-lg-6"}
         ;[:div {:class "ribbon-info text-uppercase"} "Fresh"]
         ;[:div {:class "ribbon-primary text-uppercase"} "Sale"] ; "Sale" if < price orig
        ; [:div {:data-slider-id "1", :class "owl-carousel items-slider owl-drag"}
         [:div {:class "owl-carousel" :style {:display "block" :paddingLeft "50px"}}

         [:div {:class "item"}
           [:img {:src (:image variant), :alt "shirt"}]]
          ;[:div {:class "item"}
          ; [:img {:src "img/shirt-black.png", :alt "shirt"}]]
          ;[:div {:class "item"}
          ; [:img {:src "img/shirt-green.png", :alt "shirt"}]]
          ;[:div {:class "item"}
          ; [:img {:src "img/shirt-red.png", :alt "shirt"}]]
          ]
         ; ]
         ;[:div {:data-slider-id "1", :class "owl-thumbs d-flex align-items-center justify-content-center"}
         ; [:button {:class "owl-thumb-item"}
         ;  [:img {:src "img/shirt-small.png", :alt "shirt"}]]
         ; [:button {:class "owl-thumb-item active"}
         ;  [:img {:src "img/shirt-black-small.png", :alt "shirt"}]]
         ; [:button {:class "owl-thumb-item"}
         ;  [:img {:src "img/shirt-green-small.png", :alt "shirt"}]]
         ; [:button {:class "owl-thumb-item"}
         ;  [:img {:src "img/shirt-red-small.png", :alt "shirt"}]]]
         ]
        [:div {:class "details col-lg-6"}
         [:h2 (:name variant)]
         [:div {:class "d-flex align-items-center justify-content-between flex-column flex-sm-row"}
          [:ul {:class "price list-inline no-margin"}
           [:li {:class "list-inline-item current"} (currency (:price variant))]
           [:li {:class "list-inline-item original"} (when (and (:original variant) (< (:price variant) (:original variant) )) (currency (:original variant)))]]
          [:div {:class "review d-flex align-items-center"}
           [:ul {:class "rate list-inline"}
            [:li {:class "list-inline-item"}
             [:i {:class "fa fa-star-o text-primary"}]]
            [:li {:class "list-inline-item"}
             [:i {:class "fa fa-star-o text-primary"}]]
            [:li {:class "list-inline-item"}
             [:i {:class "fa fa-star-o text-primary"}]]
            [:li {:class "list-inline-item"}
             [:i {:class "fa fa-star-o text-primary"}]]
            [:li {:class "list-inline-item"}
             [:i {:class "fa fa-star-o text-primary"}]]]
           [:span {:class "text-muted"} "No reviews"]]]
         ;[:p (:description variant)]
         [:p {:dangerouslySetInnerHTML {:__html (:description variant)}}]
         [:div {:class "d-flex align-items-center justify-content-center justify-content-lg-start"}
          [quantity-selector "pdp-qty-select"]


          (when variant-selector
            [:div
             (make-selects product-id variant-selector-id variant-selector app)
             ]
          )
]
         [:ul {:class "CTAs list-inline"}
          [:li {:class "list-inline-item"}
           [:a {:href "#", :class (if (and is-variant? (not= (count valid-variants) 1)) "btn btn-template wide disabled" "btn btn-template wide")
                :on-click (fn[e]
                            (. e preventDefault)
                            (println (str "Adding -> " (if is-variant? (:id (first valid-variants)) product-id)))
                            (add-to-cart (if is-variant? (:id (first valid-variants)) product-id) app (int-input-value "pdp-qty-select"))
                            )}
            [:i {:class "icon-cart"}] "Add to Cart" ]]
          [:li {:class "list-inline-item"}
           [:a {:href "#", :class "btn btn-template-outlined wide"}
            [:i {:class "fa fa-heart-o"}] "Add to wishlist"]]]]]]]
     [:section {:class "product-description no-padding"}
      ;[:div {:class "container"}
      ; [:ul {:role "tablist", :class "nav nav-tabs"}
      ;  [:li {:class "nav-item"}
      ;   [:a {:data-toggle "tab", :href "#description", :role "tab", :class "nav-link active"} "Description"]]
      ;  [:li {:class "nav-item"}
      ;   [:a {:data-toggle "tab", :href "#additional-information", :role "tab", :class "nav-link"} "Additional Information"]]]
      ; [:div {:class "tab-content"}
      ;  [:div {:id "description", :role "tabpanel", :class "tab-pane active"}
      ;   [:p "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. LOLUt enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. LOLDuis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
      ;   [:p "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. LOLUt enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. LOLDuis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]]
      ;  [:div {:id "additional-information", :role "tabpanel", :class "tab-pane"}
      ;   [:ul {:class "list-unstyled additional-information"}
      ;    [:li {:class "d-flex justify-content-between"}
      ;     [:strong "Compsition:"]
      ;     [:span "Cottom"]]
      ;    [:li {:class "d-flex justify-content-between"}
      ;     [:strong "Styles:"]
      ;     [:span "Casual"]]
      ;    [:li {:class "d-flex justify-content-between"}
      ;     [:strong "Properties:"]
      ;     [:span "Short Sleeve"]]
      ;    [:li {:class "d-flex justify-content-between"}
      ;     [:strong "Brand:"]
      ;     [:span "Calvin Klein"]]]]]]
      ;[:div {:class "container-fluid"}
      ; [:div {:class "share-product gray-bg d-flex align-items-center justify-content-center flex-column flex-md-row"}
      ;  [:strong {:class "text-uppercase"} "Share this on"]
      ;  [:ul {:class "list-inline text-center"}
      ;   [:li {:class "list-inline-item"}
      ;    [:a {:href "#", :target "_blank", :title "twitter"}
      ;     [:i {:class "fa fa-twitter"}]]]
      ;   [:li {:class "list-inline-item"}
      ;    [:a {:href "#", :target "_blank", :title "facebook"}
      ;     [:i {:class "fa fa-facebook"}]]]
      ;   [:li {:class "list-inline-item"}
      ;    [:a {:href "#", :target "_blank", :title "instagram"}
      ;     [:i {:class "fa fa-instagram"}]]]
      ;   [:li {:class "list-inline-item"}
      ;    [:a {:href "#", :target "_blank", :title "pinterest"}
      ;     [:i {:class "fa fa-pinterest"}]]]
      ;   [:li {:class "list-inline-item"}
      ;    [:a {:href "#", :target "_blank", :title "vimeo"}
      ;     [:i {:class "fa fa-vimeo"}]]]]]]
      ]
     ;[:section {:class "related-products"}
     ; [:div {:class "container"}
     ;  [:header {:class "text-center"}
     ;   [:h2
     ;    [:small "Similar Items"] "You may also like"]]
     ;  [:div {:class "row"}
     ;   [:div {:class "item col-lg-3"}
     ;    [:div {:class "product is-gray"}
     ;     [:div {:class "image d-flex align-items-center justify-content-center"}
     ;      [:img {:src "img/hoodie-woman-1.png", :alt "...", :class "img-fluid"}]
     ;      [:div {:class "hover-overlay d-flex align-items-center justify-content-center"}
     ;       [:div {:class "CTA d-flex align-items-center justify-content-center"}
     ;        [:a {:href "#", :class "add-to-cart"}
     ;         [:i {:class "fa fa-shopping-cart"}]]
     ;        [:a {:href "detail.html", :class "visit-product active"}
     ;         [:i {:class "icon-search"}] "View"]
     ;        [:a {:href "#", :data-toggle "modal", :data-target "#exampleModal", :class "quick-view"}
     ;         [:i {:class "fa fa-arrows-alt"}]]]]]
     ;     [:div {:class "title"}
     ;      [:a {:href "#"}
     ;       [:h3 {:class "h6 text-uppercase no-margin-bottom"} "Elegant Gray"]]
     ;      [:span {:class "price"} "$40.00"]]]]
     ;   [:div {:class "item col-lg-3"}
     ;    [:div {:class "product is-gray"}
     ;     [:div {:class "image d-flex align-items-center justify-content-center"}
     ;      [:img {:src "img/hoodie-woman-2.png", :alt "...", :class "img-fluid"}]
     ;      [:div {:class "hover-overlay d-flex align-items-center justify-content-center"}
     ;       [:div {:class "CTA d-flex align-items-center justify-content-center"}
     ;        [:a {:href "#", :class "add-to-cart"}
     ;         [:i {:class "fa fa-shopping-cart"}]]
     ;        [:a {:href "detail.html", :class "visit-product active"}
     ;         [:i {:class "icon-search"}] "View"]
     ;        [:a {:href "#", :data-toggle "modal", :data-target "#exampleModal", :class "quick-view"}
     ;         [:i {:class "fa fa-arrows-alt"}]]]]]
     ;     [:div {:class "title"}
     ;      [:a {:href "#"}
     ;       [:h3 {:class "h6 text-uppercase no-margin-bottom"} "Elegant Black"]]
     ;      [:span {:class "price"} "$40.00"]]]]
     ;   [:div {:class "item col-lg-3"}
     ;    [:div {:class "product is-gray"}
     ;     [:div {:class "image d-flex align-items-center justify-content-center"}
     ;      [:img {:src "img/hoodie-woman-3.png", :alt "...", :class "img-fluid"}]
     ;      [:div {:class "hover-overlay d-flex align-items-center justify-content-center"}
     ;       [:div {:class "CTA d-flex align-items-center justify-content-center"}
     ;        [:a {:href "#", :class "add-to-cart"}
     ;         [:i {:class "fa fa-shopping-cart"}]]
     ;        [:a {:href "detail.html", :class "visit-product active"}
     ;         [:i {:class "icon-search"}] "View"]
     ;        [:a {:href "#", :data-toggle "modal", :data-target "#exampleModal", :class "quick-view"}
     ;         [:i {:class "fa fa-arrows-alt"}]]]]]
     ;     [:div {:class "title"}
     ;      [:a {:href "#"}
     ;       [:h3 {:class "h6 text-uppercase no-margin-bottom"} "Elegant Blue"]]
     ;      [:span {:class "price"} "$40.00"]]]]
     ;   [:div {:class "item col-lg-3"}
     ;    [:div {:class "product is-gray"}
     ;     [:div {:class "image d-flex align-items-center justify-content-center"}
     ;      [:img {:src "img/hoodie-woman-4.png", :alt "...", :class "img-fluid"}]
     ;      [:div {:class "hover-overlay d-flex align-items-center justify-content-center"}
     ;       [:div {:class "CTA d-flex align-items-center justify-content-center"}
     ;        [:a {:href "#", :class "add-to-cart"}
     ;         [:i {:class "fa fa-shopping-cart"}]]
     ;        [:a {:href "detail.html", :class "visit-product active"}
     ;         [:i {:class "icon-search"}] "View"]
     ;        [:a {:href "#", :data-toggle "modal", :data-target "#exampleModal", :class "quick-view"}
     ;         [:i {:class "fa fa-arrows-alt"}]]]]]
     ;     [:div {:class "title"}
     ;      [:a {:href "#"}
     ;       [:h3 {:class "h6 text-uppercase no-margin-bottom"} "Elegant Lake"]]
     ;      [:span {:class "price"} "$40.00"]]]]]]]

     ]
    )
  )
(defn availability [app]

  ;{StoreId: "DR003", Line1: "Demo Store Melbourne", Line2: "300 Collingwood street", City: "MELBOURNE", County: "VIC", …}

;{StoreId DR004, Line1 Demo Store Sydney, Line2 200 Pitt Street, City SYDNEY, County NSW, Postcode 2000}
  (net/log "Availability::rendering component")
  (let [make-row (partial cart-availability-row app)
        selected-store (js->clj (aget js/window "storeAddress" ))]
    (println selected-store)
    ;(println cart-items)
    ;[ { :skuRef "thesku" :requestedQuantity xx } ]

    [:div
    [:div {:style {:paddingLeft "40px"} :class " row"}
     [:span {:class "col-md-6"}
      [:h6 {:class "text-uppercase"} "Pickup Location"] [:span {:class "total"} (str (get selected-store "Line1" ) " -- " (get selected-store "Line2" ))] ]]
     ;[:h6 {:class "text-uppercase"} "Address"][:p (str (get selected-store "Line1" ) " -- " (get selected-store "Line2" ))]]
    [:section {:class "checkout"}
     (when (:spinner-fo @app)
       [:div [:img {:src "img/loading_128.GIF"}]]
       )
     [:div ]
     ;[:div {:class " row"}
     ; [:span {:class "col-md-10 col-2"}
     ;  [:p (str "Selected Store: " (get selected-store "StoreId")) ]
     ;  [:p (str " Address: " (get selected-store "Line1" ) " -- " (get selected-store "Line2" ))]]]
     ;[:div [:p (str "Selected Store: " (get selected-store "StoreId") " Address: " (get selected-store "Line1" ) " -- " (get selected-store "Line2" ))]]
     [:div {:class "container"}
      [:div {:class "row"}
       [:div {:class "col-lg-8"}
        [:ul {:class "nav nav-pills"}
         [:li {:class "nav-item"}
          [:a {:href "#checkout-store", :class "nav-link"} "Choose Store"]]
         [:li {:class "nav-item"}
          [:a {:href "#checkout-shipping", :class "nav-link active"} "Check Availability"]]
         [:li {:class "nav-item"}
          [:a {:href "#checkout-payment", :class "nav-link"} "Payment Method "]]
         ]
        [:div {:class "tab-content"}
         [:div {:id "order-review", :class "tab-block"}
          [:div {:class "cart"}
           [:div {:class "cart-holder"}
            [:div {:class "basket-header"}
             [:div {:class "row"}
              [:div {:class "col-6"} "Product"]
              [:div {:class "col-2"} "Price"]
              [:div {:class "col-2"} "Quantity"]
              [:div {:class "col-2"} "Unit Price"]]]
            [:div {:class "basket-body"}
             (map make-row (vals (:cart @app)))
             ]]
           [:div {:class "total row"}
            [:span {:class "col-md-10 col-2"} "Total"]
            [:span {:class "col-md-2 col-10 text-primary"} (currency (calc-cart-item-total app))]]]
          [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
           [:a {:href "#checkout-store", :class "btn btn-template-outlined wide prev"}
            [:i {:class "fa fa-angle-left"}]"Back to Choose Store"]
           [:a {:href "#checkout-payment", :class "btn btn-template wide next"} "Payment Method"
            [:i {:class "fa fa-angle-right"}]]]]]]
       [order-summary app]
       ]]]]))

(defn checkout [app]

  [:section {:class "checkout"}
   [:div {:class "container"}
    [:div {:class "row"}
     [:div {:class "col-lg-8"}
      [:ul {:class "nav nav-pills"}
       [:li {:class "nav-item"}
        [:a {:href "#checkout-address", :class "nav-link active"} "Address"]]
       [:li {:class "nav-item"}
        [:a {:href "#", :class "nav-link disabled"} "Shipping"]]
       [:li {:class "nav-item"}
        [:a {:href "#", :class "nav-link disabled"} "Payment Method "]]
       [:li {:class "nav-item"}
        [:a {:href "#", :class "nav-link disabled"} "Order Review"]]]
      [:div {:class "tab-content"}

       [:div {:class "CTAss"}
       [:a {:href "#checkout-address", :class "btn btn-template-outlined wide prev" :on-click #(copy-address app)}
        [:i {:class "fa"}] "Use Saved Address"]
       ]

       [:div {:id "address", :class "active tab-block"}

        [:form {:action "#"}
         [:div {:class "row"}
          [text-input "firstname" "First Name" "first-name" "Enter your first name" "col-md-6" app :address]
          [text-input "lastname" "Last Name" "last-name" "Enter your last name" "col-md-6" app :address]
          [text-input "email" "Email Address" "email" "Enter your email address" "col-md-6" app :address]
          [text-input "street" "Street" "street" "Enter your street address" "col-md-6" app :address]
          [text-input "city" "City" "city" "Your city" "col-md-3" app :address]
          [text-input "state" "State" "state" "Your state" "col-md-3" app :address]
          [text-input "zip" "Postal Code" "zip" "Your Postal Code" "col-md-3" app :address]
          [text-input "country" "Country" "country" "Your country" "col-md-3" app :address]
          [text-input "phone-number" "Phone Number" "phone-number" "Enter your phone number" "col-md-6" app :address]
          ]]

        [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
         [:a {:href "#checkout-delivery", :class "btn btn-template-outlined wide prev"}
          [:i {:class "fa fa-angle-left"}] "Delivery Option"]
         [:a {:href "#checkout-shipping", :class "btn btn-template wide next"} "Choose shipping method"
          [:i {:class "fa fa-angle-right"}]]]]]]
     [order-summary app]
     ]]])

(defn update-shipping [method amount app]
  (swap! app assoc :shipping amount)
  (swap! app assoc :shipping-method method)
  )

(defn shipping[app]
  [:section {:class "checkout"}
   [:div {:class "container"}
    [:div {:class "row"}
     [:div {:class "col-lg-8"}
      [:ul {:class "nav nav-pills"}
       [:li {:class "nav-item"}
        [:a {:href "#checkout-address", :class "nav-link"} "Address"]]
       [:li {:class "nav-item"}
        [:a {:href "#checkout-shipping", :class "nav-link active"} "Shipping"]]
       [:li {:class "nav-item"}
        [:a {:href "#", :class "nav-link disabled"} "Payment Method "]]
       [:li {:class "nav-item"}
        [:a {:href "#", :class "nav-link disabled"} "Order Review"]]]
      [:div {:class "tab-content"}
       [:div {:id "delivery-method", :class "tab-block"}
        [:form {:action "#", :class "shipping-form"}
         [:div {:class "row"}
          [:div {:class "form-group col-md-6"}
           [:input {:type "radio", :name "shippping", :id "option1", :class "radio-template" :on-click #(update-shipping :EXPRESS 15 app)}]
           [:label {:for "option1"}
            [:strong "Next Day Shipping"]
            [:br]
            [:span {:class "label-description"} "Fastest Option"]]]
          [:div {:class "form-group col-md-6"}
           [:input {:type "radio", :name "shippping", :id "option2", :class "radio-template" :on-click #(update-shipping :STANDARD 5 app)}]
           [:label {:for "option2"}
            [:strong "Standard Shipping"]
            [:br]
            [:span {:class "label-description"} "Two business days"]]]
          ;[:div {:class "form-group col-md-6"}
          ; [:input {:type "radio", :name "shippping", :id "option3", :class "radio-template"}]
          ; [:label {:for "option3"}
          ;  [:strong "Usps next day"]
          ;  [:br]
          ;  [:span {:class "label-description"} "Get it right on next day - fastest option possible."]]]
          ;[:div {:class "form-group col-md-6"}
          ; [:input {:type "radio", :name "shippping", :id "option4", :class "radio-template"}]
          ; [:label {:for "option4"}
          ;  [:strong "Usps next day"]
          ;  [:br]
          ;  [:span {:class "label-description"} "Get it right on next day - fastest option possible."]]]
          ]]
        [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
         [:a {:href "#checkout-address", :class "btn btn-template-outlined wide prev"}
          [:i {:class "fa fa-angle-left"}]"Back to Address"]
         [:a {:href "#checkout-payment", :class "btn btn-template wide next"} "Choose payment method"
          [:i {:class "fa fa-angle-right"}]]]]]]
     [order-summary app]

     ]]])

(def place-order-chan (chan))

(defn place-order-event-loop [app-state]
  (go-loop []
           (when-let [response (<! place-order-chan)]
             (net/log "received data on place-order channel")
             (net/log response)
             (if (= (:status response) 200)
               (do
                 (swap! app-state assoc :order-id (:body response))
                 (swap! app-state assoc :cart {})
                 (swap! app-state assoc :order-error nil)
                 )
               (swap! app-state merge @app-state {:order-error (format-error response)})
               )
             (recur)
             )
           )
  )

(defn build-customer [first-name last-name mobile email]
  {"firstName" first-name "lastName" last-name "mobile" mobile "email" email}
  )

(defn build-cc-customer [payment]
  (let [pmt-field (partial get payment)
        name (pmt-field "card-name")
        name-parts (clojure.string/split name " ")
        first-name (first name-parts)
        last-name (last name-parts)
        phone (pmt-field  "phone-number")
        email (pmt-field "email")]

    (build-customer first-name last-name phone email)
    )
  )

(defn build-hd-customer [address]
  (let [field-list ["firstname" "lastname" "phone-number" "email"]]
    (apply build-customer (map (partial get address) field-list))
    ))

(defn build-shipping-address [address]
  (let [adr-field (partial get address)]
    {
     "name"     (str (adr-field "first-name") " " (adr-field "last-name"))
     "street"   (adr-field "street")
     "city"     (adr-field "city")
     "state"    (adr-field "state")
     "postcode" (adr-field "zip")
     "country"  (adr-field "country")
     }
    )
  )


(defn make-order [app]
  (let [payment (:payment @app)
        address (:address @app)
        delivery (:delivery @app)
        ship-cost (:shipping @app)
        ship-method (:shipping-method @app)
        customer (if (= delivery :CC) (build-cc-customer payment) (build-hd-customer address))
        selected-store (get (js->clj (aget js/window "storeAddress")) "StoreId") ;"HB001" ;
        items (vec (map (fn [item] {"skuRef" (:sku item) "skuPrice" (:price item) "requestedQty" (:quantity item) "totalPrice" (* (:price item) (:quantity item))}) (vals (:cart @app))))]

    (merge {
            "customer"   customer
            "items"      items
            "orderRef"   (str (rand-int 1000000))
            "retailerId" 1}
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

(defn place-order [app]
  (let [order (make-order app)]
    (println (with-out-str (pp/pprint order)))
    (swap! app assoc :order-id nil)
    (swap! app assoc :order-error nil)
    (net/place-order order place-order-chan)
    )
  )

(defn copy-payment[app]
  (swap! app assoc-in [:payment] (:saved-payment @app)
         false
         ))

(defn payment[app]
  (let [hd (= (:delivery @app) :HD) cc (= (:delivery @app) :CC)]
    [:section {:class "checkout"}
     [:div {:class "container"}
      [:div {:class "row"}
       [:div {:class "col-lg-8"}
        [:ul {:class "nav nav-pills"}
         (when hd
           [:li {:class "nav-item"}
            [:a {:href "#checkout-address", :class "nav-link"} "Address"]])
         (when hd
           [:li {:class "nav-item"}
            [:a {:href "#checkout-shipping", :class "nav-link"} "Shipping"]]
           )
         (when cc
           [:li {:class "nav-item"}
            [:a {:href "#checkout-store", :class "nav-link"} "Choose Store"]]
           )
         (when cc
           [:li {:class "nav-item"}
            [:a {:href "#checkout-availability", :class "nav-link"} "Check Availability"]]
           )
         [:li {:class "nav-item"}
          [:a {:href "#checkout-payment", :class "nav-link active"} "Payment Method "]]
         (when hd
           [:li {:class "nav-item"}
            [:a {:href "#", :class "nav-link disabled"} "Order Review"]]
           )
         ]
        [:div {:class "tab-content"}
         [:div {:id "payment-method", :class "tab-block"}
          [:div {:id "accordion", :role "tablist", :aria-multiselectable "true"}
           [:div {:class "card"}
            [:div {:id "headingOne", :role "tab", :class "card-header"}
             [:h6
              [:a {:data-toggle "collapse", :data-parent "#accordion", :href "#collapseOne", :aria-expanded "true", :aria-controls "collapseOne"} "Credit Card"]]]
            [:div {:id "collapseOne", :role "tabpanel", :aria-labelledby "headingOne", :class "collapse show"}
             [:div {:class "card-body"}

              [:div {:class "CTAss"}
               [:a {:href "#checkout-payment", :class "btn btn-template-outlined wide prev" :on-click #(copy-payment app)}
                [:i {:class "fa"}] "Use Saved Payment"]
               ]

              [:form {:action "#" :autocomplete "false"}
               [:div {:class "row"}
                [text-input "card-name" "Name on Card" "card-name" "Name on Card" "col-md-6" app :payment]
                [text-input "card-number" "Card Number" "card-number" "Card Number" "col-md-6" app :payment]
                [text-input "expiry-date" "Expiration Date" "expiry-date" "MM/YY" "col-md-4" app :payment]
                [text-input "card-cvv" "CVC/CVV" "card-cvv" "123" "col-md-4" app :payment]
                [text-input "card-zip" "Billing Postal Code" "card-zip" "Postal Code" "col-md-4" app :payment]
                (when cc
                  [text-input "phone-number" "Phone Number" "phone-number" "Enter your phone number" "col-md-6" app :payment]
                  )
                (when cc
                  [text-input "email" "Email Address" "email" "Enter your email address" "col-md-6" app :payment]
                  )
                ]]]]]
           [:div {:class "card"}
            [:div {:id "headingTwo", :role "tab", :class "card-header"}
             [:h6
              [:a {:data-toggle "collapse", :data-parent "#accordion", :href "#collapseTwo", :aria-expanded "false", :aria-controls "collapseTwo", :class "collapsed"} "Paypal"]]]
            [:div {:id "collapseTwo", :role "tabpanel", :aria-labelledby "headingTwo", :class "collapse"}
             [:div {:class "card-body"}
              [:input {:type "radio", :name "shippping", :id "payment-method-1", :class "radio-template"}]
              [:label {:for "payment-method-1"}
               [:strong "Continue with Paypal"]
               [:br]
               [:span {:class "label-description"} "Lorem ipsum dolor sit amet, consectetur adipisicing elit."]]]]]
           [:div {:class "card"}
            [:div {:id "headingThree", :role "tab", :class "card-header"}
             [:h6
              [:a {:data-toggle "collapse", :data-parent "#accordion", :href "#collapseThree", :aria-expanded "false", :aria-controls "collapseThree", :class "collapsed"} "Pay on delivery"]]]
            [:div {:id "collapseThree", :role "tabpanel", :aria-labelledby "headingThree", :class "collapse"}
             [:div {:class "card-body"}
              [:input {:type "radio", :name "shippping", :id "payment-method-2", :class "radio-template"}]
              [:label {:for "payment-method-2"}
               [:strong "Pay on Delivery"]
               [:br]
               [:span {:class "label-description"} "Lorem ipsum dolor sit amet, consectetur adipisicing elit."]]]]]]
          [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}


           (when hd
             [:a {:href "#checkout-shipping", :class "btn btn-template-outlined wide prev"}
              [:i {:class "fa fa-angle-left"}] "Back to delivery method"]
             )
           (when hd
             [:a {:href "#checkout-summary", :class "btn btn-template wide next"} "Continue to order review"
              [:i {:class "fa fa-angle-right"}]]
             )
           (when cc
             [:a {:href "#checkout-availability", :class "btn btn-template-outlined wide prev"}
              [:i {:class "fa fa-angle-left"}] "Back to Check Availability"])
           (when cc
             [:a {:href "#checkout-placeorder", :class "btn btn-template wide next" :on-click #(place-order app)} "Place Order"
              [:i {:class "fa fa-angle-right"}]]
             )
           ]]]]
       [order-summary app]
       ]]]
    )
  )

(defn cart-review-row[app cart-item]
  (let [{:keys [:thumbnail :price :quantity :key :description :sku]} cart-item]
    ^{:key (str "cri-" key)}
    [:div {:class "item row d-flex align-items-center"}
     [:div {:class "col-6"}
      [:div {:class "d-flex align-items-center"}
       [:img {:src thumbnail, :alt "...", :class "img-fluid"}]
       [:div {:class "title"}
        [:a {:href "detail.html"}
         [:h6 description]
         [:span {:class "text-muted"} (str "SKU: " sku)]]]]]
     [:div {:class "col-2"}
      [:span (currency price)]]
     [:div {:class "col-2"}
      [:span quantity]]
     [:div {:class "col-2"}
      [:span (currency (* price quantity))]]]
    ))

(defn cart-review [app]
  (let [make-row (partial cart-review-row app)]
  [:section {:class "checkout"}
   [:div {:class "container"}
    [:div {:class "row"}
     [:div {:class "col-lg-8"}
      [:ul {:class "nav nav-pills"}
       [:li {:class "nav-item"}
        [:a {:href "#checkout-address", :class "nav-link"} "Address"]]
       [:li {:class "nav-item"}
        [:a {:href "#checkout-shipping", :class "nav-link"} "Shipping"]]
       [:li {:class "nav-item"}
        [:a {:href "#checkout-payment", :class "nav-link"} "Payment Method "]]
       [:li {:class "nav-item"}
        [:a {:href "#checkout-summary", :class "nav-link active"} "Order Review"]]]
      [:div {:class "tab-content"}
       [:div {:id "order-review", :class "tab-block"}
        [:div {:class "cart"}
         [:div {:class "cart-holder"}
          [:div {:class "basket-header"}
           [:div {:class "row"}
            [:div {:class "col-6"} "Product"]
            [:div {:class "col-2"} "Price"]
            [:div {:class "col-2"} "Quantity"]
            [:div {:class "col-2"} "Unit Price"]]]
          [:div {:class "basket-body"}
           (map make-row (vals (:cart @app)))
           ]]
         [:div {:class "total row"}
          [:span {:class "col-md-10 col-2"} "Total"]
          [:span {:class "col-md-2 col-10 text-primary"} (currency (calc-cart-item-total app))]]]
        [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
         [:a {:href "#checkout-payment", :class "btn btn-template-outlined wide prev"}
          [:i {:class "fa fa-angle-left"}]"Back to payment method"]
         [:a {:href "#checkout-placeorder", :class "btn btn-template wide next" :on-click #(place-order app)} "Place Order"
          [:i {:class "fa fa-angle-right"}]]]]]]
     [order-summary app]
     ]]]))

(defn order-confirmation[app]
  [:section {:class "padding-small"}
   [:div {:class "container"}
    [:div {:class "row about-item"}
     [:div {:class "col-lg-8 col-sm-9"}

      (if (:order-error @app)
        [:h2 "There was a problem processing your order"[:p {:style {"color" "#b21129"}} (:order-error @app)]]
        (if (:order-id @app)
          [:h2 "Your order has been placed successfully"]
          [:h2 "Your order is processing"]
          )
        )
     ]
     [:div {:class "col-lg-4 col-sm-3 d-none d-sm-flex align-items-center"}
      [:div {:class "about-icon ml-lg-0"}
       [:img (when (nil? (:order-id @app)) {:src "img/loading_128.GIF"})]]]]]]
  )