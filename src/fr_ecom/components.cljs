(ns fr-ecom.components
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs.pprint :as pp]
    [clojure.string :as string]
    [goog.string :as gstring]
    [goog.string.format]
    [goog.date.DateTime]
    [reagent.format :as fmt]
    [clojure.string]
    [reagent.core :as reagent]
    [fr-api.network :as net]
    [fr-api.data-source :as ds]
    [fr-api.subscriptions :as subscriptions]
    [cljs.core.async :refer [<! put! chan]]
    [re-frame.core :refer [subscribe dispatch]]
    [cljs-time.coerce]
    [cljs-time.format :as tf]
    )
  )

;TODO see check about link processing being disabled in some cases - e.g.:
;(fn [e]
;  (.(.preventDefault e)Default e)

;(defn build-cart[products next-key!]
;  (reduce (fn [col item]
;            (let [item-key (next-key!)
;                  keyed-item (assoc item :key item-key)]
;              (assoc col item-key keyed-item)
;              )
;            ) {} products)
;  )

(defn log [msg]
  (.log js/console (str msg))
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

(defn sites-row[site]
  ^{:key (gensym "cs")}
  [:a {:href "#", :class "dropdown-item" :on-click (fn[e] (do (. e preventDefault)(net/log (str "Processing..." (:id site))) (dispatch [:ecom/load-site-config (:id site)])))} (:name site)]
  )

(defn navbar' []
  (let [sites @(subscribe [:ecom/sites])
        site-abbrev @(subscribe [:ecom/active-site-abbreviation])]
    (net/log (str "Sites subscription: [" sites "]"))
    [:div {:class "top-bar"}
     [:div {:class "container-fluid"}
      [:div {:class "row d-flex align-items-center"}
       [:div {:class "col-lg-6 hidden-lg-down text-col"}
        [:ul {:class "list-inline"}
         [:li {:class "list-inline-item"}
          [:i {:class "icon-telephone"}] "020-800-456-747"]
         [:li {:class "list-inline-item"} "Free shipping on orders over $300"]]]
       [:div {:class "col-lg-6 d-flex justify-content-end"}  ;<!-- Language Dropdown-->
        [:div {:class "dropdown show"}
         [:a {:id "langsDropdown", :href "https://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
          [:img {:src "img/united-kingdom.svg", :alt "english"}] "English"]
         [:div {:aria-labelledby "langsDropdown", :class "dropdown-menu"}
          [:a {:href "#", :class "dropdown-item"}
           [:img {:src "img/germany.svg", :alt "german"}] "German"]
          [:a {:href "#", :class "dropdown-item"}
           [:img {:src "img/france.svg", :alt "french"}] "French"]]] ;"<!-- Currency Dropdown-->"
        [:div {:class "dropdown show"}
         [:a {:id "currencyDropdown", :href "#", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"} "USD"]
         [:div {:aria-labelledby "currencyDropdown", :class "dropdown-menu"}
          [:a {:href "#", :class "dropdown-item"} "EUR"]
          [:a {:href "#", :class "dropdown-item"} "GBP"]]]
        [:div {:class "dropdown show"}
         [:a {:id "sitedropdown", :href "#", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"} site-abbrev] ;<--current site inside here...
         [:div {:aria-labelledby "currencyDropdown", :class "dropdown-menu"}

          (map sites-row sites)

          ]]]]]])
  )

;TODO attach the formatter to the site configuration (US vs UK AUS etc.)
(defn date[val]
  (let [date-val (cljs-time.coerce/from-string val)]
    (tf/unparse (tf/formatters :date) date-val)
    )
  )

(defn currency[val]
  (fmt/currency-format val)
  )

(defn user-link[user]
  ^{:key (gensym "cx")}
  [:li {:class "dropdown-item"}
   [:a {:on-click #(dispatch [:ecom/switch-user user])} (str (:firstname user) " " (:lastname user))]]
  )

(defn mini-cart-row[cart-item]
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
    [:i {:class "fa fa-trash-o" :on-click #(dispatch [:ecom/remove-cart-item key])}]]]]]
    ))

(defn menu' []

  (let [cart-item-count @(subscribe [:ecom/cart-items-count])
        cart-total @(subscribe [:ecom/cart-subtotal])
        top-level-categories @(subscribe [:ecom/top-level-categories])
        all-categories @(subscribe [:ecom/categories])
        logged-in? @(subscribe [:ecom/logged-in?])
        site-users @(subscribe [:ecom/site-users])
        ]
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
        (for [category top-level-categories]
          ^{:key (str (gensym "tn-"))}
          [:li {:class "nav-item dropdown"}
           [:a {:id            "navbarDropdownMenuLink", :href "http://example.com", :data-toggle "dropdown",
                :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} (:name category)
            [:i {:class "fa fa-angle-down"}]]

           [:ul {:aria-labelledby "navbarDropdownMenuLink", :class "dropdown-menu"}

            (for [child (concat (vec @(subscribe [:ecom/category-by-parent (:ref category)]))
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

          (map user-link site-users)

          (when logged-in?
            (let [current-user @(subscribe [:ecom/current-user])
                  customer-name (str (:firstname current-user) " " (:lastname current-user))]
              [:li {:class "dropdown-item"}
               [:a {:href "#order-history" :on-click #(dispatch [:ecom/load-orders {:query customer-name}])} "Orders       "]]
              )
            )

          [:li {:class "dropdown-divider"} "     "]

          (when logged-in?
            [:li {:class "dropdown-item"}
             [:a {:href "#"} "Logout       "]]
            )

          ]] ;"<!-- Cart Dropdown-->"

        [:div {:class "cart dropdown show"}
         [:a {:id "cartdetails", :href "#cart", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
          [:i {:class "icon-cart"}]
          [:div {:class "cart-no"} cart-item-count ]] ;(reduce + (map :quantity (vals (:cart @app))))
         [:a {:href "#cart", :class "text-primary view-cart"} "View Cart"]
         [:div {:aria-labelledby "cartdetails", :class "dropdown-menu"}  ;"<!-- cart item-->"
          [:div {:style {:overflow "auto" :height "280px"}}
           (let [cart-items @(subscribe [:ecom/cart-items])]
             (map mini-cart-row cart-items)
             )
           ]
          [:div {:class "dropdown-item total-price d-flex justify-content-between"}
           [:span "Total"]
           [:strong {:class "text-primary"} (currency cart-total)]] ;"<!-- call to actions-->"

          [:div {:class "dropdown-item CTA d-flex"}
           [:a {:href "#cart", :class "btn btn-template wide"} "View Cart"]
           [:a {:href "#checkout-delivery", :class "btn btn-template wide"} "Checkout"]]]]]]]])
  )

;(def menu
;  [:nav {:class "navbar navbar-expand-lg"}
;   [:div {:class "search-area"}
;    [:div {:class "search-area-inner d-flex align-items-center justify-content-center"}
;     [:div {:class "close-btn"}
;      [:i {:class "icon-close"}]]
;     [:form {:action "#"}
;      [:div {:class "form-group"}
;       [:input {:type "search", :name "search", :id "search", :placeholder "What are you looking for?"}]
;       [:button {:type "submit", :class "submit"}
;        [:i {:class "icon-search"}]]]]]]
;   [:div {:class "container-fluid"}  ; "<!-- Navbar Header  -->"
;    [:a {:href "index.html", :class "navbar-brand"}
;     [:img {:src "https://fluentcommerce.com/assets/images/fluentcommerce.svg", :alt "..."}]] ;img/logo.png
;    [:button {:type "button", :data-toggle "collapse", :data-target "#navbarCollapse", :aria-controls "navbarCollapse", :aria-expanded "false", :aria-label "Toggle navigation", :class "navbar-toggler navbar-toggler-right"}
;     [:i {:class "fa fa-bars"}]] ;"<!-- Navbar Collapse -->"
;    [:div {:id "navbarCollapse", :class "collapse navbar-collapse"}
;     [:ul {:class "navbar-nav mx-auto"}
;      [:li {:class "nav-item"}
;       [:a {:href "index.html", :class "nav-link active"} "Home"]]
;      [:li {:class "nav-item"}
;       [:a {:href "category.html", :class "nav-link"} "Shop"]] ;"<!-- Megamenu-->"
;      [:li {:class "nav-item dropdown menu-large"}
;       [:a {:href "#", :data-toggle "dropdown", :class "nav-link"} "Template"
;        [:i {:class "fa fa-angle-down"}]]
;       [:div {:class "dropdown-menu megamenu"}
;        [:div {:class "row"}
;         [:div {:class "col-lg-9"}
;          [:div {:class "row"}
;           [:div {:class "col-lg-3"}
;            [:strong {:class "text-uppercase"} "Home"]
;            [:ul {:class "list-unstyled"}
;             [:li
;              [:a {:href "index.html"} "Homepage 1"]]]
;            [:strong {:class "text-uppercase"} "Shop"]
;            [:ul {:class "list-unstyled"}
;             [:li
;              [:a {:href "#categories"} "Category - left sidebar"]]
;             [:li
;              [:a {:href "category-right.html"} "Category - right sidebar"]]
;             [:li
;              [:a {:href "category-full.html"} "Category - full width"]]
;             [:li
;              [:a {:href "#hello"} "Product detail"]]]] ;detail.html
;           [:div {:class "col-lg-3"}
;            [:strong {:class "text-uppercase"} "Order process"]
;            [:ul {:class "list-unstyled"}
;             [:li
;              [:a {:href "#cart"} "Shopping cart"]]
;             [:li
;              [:a {:href "#checkout-address"} "Checkout 1 - Address"]]
;             [:li
;              [:a {:href "#checkout-shipping"} "Checkout 2 - Delivery"]]
;             [:li
;              [:a {:href "#checkout-payment"} "Checkout 3 - Payment"]]
;             [:li
;              [:a {:href "#checkout-summary"} "Checkout 4 - Confirmation"]]]
;            [:strong {:class "text-uppercase"} "Blog"]
;            [:ul {:class "list-unstyled"}
;             [:li
;              [:a {:href "blog.html"} "Blog"]]
;             [:li
;              [:a {:href "post.html"} "Post"]]]]
;           [:div {:class "col-lg-3"}
;            [:strong {:class "text-uppercase"} "Pages"]
;            [:ul {:class "list-unstyled"}
;             [:li
;              [:a {:href "contact.html"} "Contact"]]
;             [:li
;              [:a {:href "about.html"} "About us"]]
;             [:li
;              [:a {:href "text.html"} "Text page"]]
;             [:li
;              [:a {:href "404.html"} "Error 404"]]
;             [:li
;              [:a {:href "500.html"} "Error 500"]]
;             [:li "More coming soon"]]]
;           [:div {:class "col-lg-3"}
;            [:strong {:class "text-uppercase"} "Some more content"]
;            [:ul {:class "list-unstyled"}
;             [:li
;              [:a {:href "#"} "Demo content"]]
;             [:li
;              [:a {:href "#"} "Demo content"]]
;             [:li
;              [:a {:href "#"} "Demo content"]]
;             [:li
;              [:a {:href "#"} "Demo content"]]
;             [:li
;              [:a {:href "#"} "Demo content"]]
;             [:li
;              [:a {:href "#"} "Demo content"]]
;             [:li
;              [:a {:href "#"} "Demo content"]]
;             [:li
;              [:a {:href "#"} "Demo content"]]]]]
;          [:div {:class "row services-block"}
;           [:div {:class "col-xl-3 col-lg-6 d-flex"}
;            [:div {:class "item d-flex align-items-center"}
;             [:div {:class "icon"}
;              [:i {:class "icon-truck text-primary"}]]
;             [:div {:class "text"}
;              [:span {:class "text-uppercase"} "Free shipping &amp; return"]
;              [:small "Free Shipping over $300"]]]]
;           [:div {:class "col-xl-3 col-lg-6 d-flex"}
;            [:div {:class "item d-flex align-items-center"}
;             [:div {:class "icon"}
;              [:i {:class "icon-coin text-primary"}]]
;             [:div {:class "text"}
;              [:span {:class "text-uppercase"} "Money back guarantee"]
;              [:small "30 Days Money Back"]]]]
;           [:div {:class "col-xl-3 col-lg-6 d-flex"}
;            [:div {:class "item d-flex align-items-center"}
;             [:div {:class "icon"}
;              [:i {:class "icon-headphones text-primary"}]]
;             [:div {:class "text"}
;              [:span {:class "text-uppercase"} "020-800-456-747"]
;              [:small "24/7 Available Support"]]]]
;           [:div {:class "col-xl-3 col-lg-6 d-flex"}
;            [:div {:class "item d-flex align-items-center"}
;             [:div {:class "icon"}
;              [:i {:class "icon-secure-shield text-primary"}]]
;             [:div {:class "text"}
;              [:span {:class "text-uppercase"} "Secure Payment"]
;              [:small "Secure Payment"]]]]]]
;         [:div {:class "col-lg-3 text-center product-col hidden-lg-down"}
;          [:a {:href "detail.html", :class "product-image"}
;           [:img {:src "img/shirt.png", :alt "...", :class "img-fluid"}]]
;          [:h6 {:class "text-uppercase product-heading"}
;           [:a {:href "detail.html"} "Lose Oversized Shirt"]]
;          [:ul {:class "rate list-inline"}
;           [:li {:class "list-inline-item"}
;            [:i {:class "fa fa-star-o text-primary"}]]
;           [:li {:class "list-inline-item"}
;            [:i {:class "fa fa-star-o text-primary"}]]
;           [:li {:class "list-inline-item"}
;            [:i {:class "fa fa-star-o text-primary"}]]
;           [:li {:class "list-inline-item"}
;            [:i {:class "fa fa-star-o text-primary"}]]
;           [:li {:class "list-inline-item"}
;            [:i {:class "fa fa-star-o text-primary"}]]]
;          [:strong {:class "price text-primary"} "$65.00"]
;          [:a {:href "#", :class "btn btn-template wide"} "Add to cart"]]]]] ;"<!-- /Megamenu end-->"
;      ; ;"<!-- Multi level dropdown    -->"
;      [:li {:class "nav-item dropdown"}
;       [:a {:id "navbarDropdownMenuLink", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "Dropdown"
;        [:i {:class "fa fa-angle-down"}]]
;       [:ul {:aria-labelledby "navbarDropdownMenuLink", :class "dropdown-menu"}
;        [:li
;         [:a {:href "#", :class "dropdown-item"} "Action"]]
;        [:li
;         [:a {:href "#", :class "dropdown-item"} "Another action"]]
;        [:li {:class "dropdown-submenu"}
;         [:a {:id "navbarDropdownMenuLink2", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "Dropdown link"
;          [:i {:class "fa fa-angle-down"}]]
;         [:ul {:aria-labelledby "navbarDropdownMenuLink2", :class "dropdown-menu"}
;          [:li
;           [:a {:href "#", :class "dropdown-item"} "Action"]]
;          [:li {:class "dropdown-submenu"}
;           [:a {:id "navbarDropdownMenuLink3", :href "http://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "nav-link"} "\n                          Another action"
;            [:i {:class "fa fa-angle-down"}]]
;           [:ul {:aria-labelledby "navbarDropdownMenuLink3", :class "dropdown-menu"}
;            [:li
;             [:a {:href "#", :class "dropdown-item"} "Action"]]
;            [:li
;             [:a {:href "#", :class "dropdown-item"} "Action"]]
;            [:li
;             [:a {:href "#", :class "dropdown-item"} "Action"]]
;            [:li
;             [:a {:href "#", :class "dropdown-item"} "Action"]]]]
;          [:li
;           [:a {:href "#", :class "dropdown-item"} "Something else here"]]]]]] ;"<!-- Multi level dropdown end-->"
;      [:li {:class "nav-item"}
;       [:a {:href "blog.html", :class "nav-link"} "Blog "]]
;      [:li {:class "nav-item"}
;       [:a {:href "contact.html", :class "nav-link"} "Contact"]]]
;     [:div {:class "right-col d-flex align-items-lg-center flex-column flex-lg-row"}  ;"<!-- Search Button-->"
;      [:div {:class "search"}
;       [:i {:class "icon-search"}]] ;"<!-- User Dropdown-->"
;      [:div {:class "user dropdown show"}
;       [:a {:id "userdetails", :href "https://example.com", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
;        [:i {:class "icon-profile"}]]
;       [:ul {:aria-labelledby "userdetails", :class "dropdown-menu"}
;        [:li {:class "dropdown-item"}
;         [:a {:href "#"} "Profile       "]]
;        [:li {:class "dropdown-item"}
;         [:a {:href "#"} "Orders       "]]
;        [:li {:class "dropdown-divider"} "     "]
;        [:li {:class "dropdown-item"}
;         [:a {:href "#"} "Logout       "]]]] ;"<!-- Cart Dropdown-->"
;      [:div {:class "cart dropdown show"}
;       [:a {:id "cartdetails", :href "#cart", :data-toggle "dropdown", :aria-haspopup "true", :aria-expanded "false", :class "dropdown-toggle"}
;        [:i {:class "icon-cart"}]
;        [:div {:class "cart-no"} "1"]]
;       [:a {:href "#cart", :class "text-primary view-cart"} "View Cart"]
;       [:div {:aria-labelledby "cartdetails", :class "dropdown-menu"}  ;"<!-- cart item-->"
;        [:div {:class "dropdown-item cart-product"}
;         [:div {:class "d-flex align-items-center"}
;          [:div {:class "img"}
;           [:img {:src "img/hoodie-man-1.png", :alt "...", :class "img-fluid"}]]
;          [:div {:class "details d-flex justify-content-between"}
;           [:div {:class "text"}
;            [:a {:href "#"}
;             [:strong "Heather Gray Hoodie"]]
;            [:small "Quantity: 1 "]
;            [:span {:class "price"} "$75.00 "]]
;           [:div {:class "delete"}
;            [:i {:class "fa fa-trash-o"}]]]]] ;"<!-- total price-->"
;        [:div {:class "dropdown-item total-price d-flex justify-content-between"}
;         [:span "Total"]
;         [:strong {:class "text-primary"} "$75.00"]] ;"<!-- call to actions-->"
;        [:div {:class "dropdown-item CTA d-flex"}
;         [:a {:href "#cart", :class "btn btn-template wide"} "View Cart"]
;         [:a {:href "#checkout-address", :class "btn btn-template wide"} "Checkout"]]]]]]]])

(defn hero-items[]

  (let [content @(subscribe[:ecom/home-page-content])]
    (log (str "Content from subscription: " content))
    (if (not-empty content)
      [:div
      content
       ]
      [:section {:class "hero hero-home no-padding"}
       [:div {:class "owl-carousel owl-theme hero-slider"}
        [:div {:style {:background "url(https://s3-us-west-1.amazonaws.com/fluent-demo-static/fluent/cloud_native_distributed_order_management_fluent_commerce.png)"} :class "item d-flex align-items-center has-pattern"}
         [:div {:class "container"}
          [:div {:class "row"}
           [:div {:class "col-lg-6"}
            [:h1 "Cloud Native Distributed Order Management"]
            [:ul {:class "lead"}
             [:li
              [:strong "Empowering merchants to rapidly out-convenience the competitition with orchestrated omnichannel software"]]
             ]
            [:a {:href "#", :class "btn btn-template wide shop-now"} "Shop Now"
             [:i {:class "icon-bag"} " "]]]]]]
        [:div {:style {:background "url(https://s3-us-west-1.amazonaws.com/fluent-demo-static/fluent/global_inventory_header_fluent_commerce.png)"} :class "item d-flex align-items-center"}
         [:div {:class "container"}
          [:div {:class "row"}
           [:div {:class "col-lg-6"}
            [:h1 "Built for distributed order management"]
            [:p {:class "lead"} "The Fluent Orchestration Cloud functionality is built on a commerce orchestration engine. It’s cloud native architecture enables continuous delivery of new platform features, the coordination and extension of all capability, and the rapid deployment of customisation with zero downtime."]
            ]]]]
        [:div {:style {:background "url(https://s3-us-west-1.amazonaws.com/fluent-demo-static/fluent/store_fulfilment_header_fluent_commerce.png)"} :class "item d-flex align-items-center"}
         [:div {:class "container"}
          [:div {:class "row"}
           [:div {:class "col-lg-6"}
            [:h1 "Store Fulfilment"]
            [:p {:class "lead"} "Convert stores into mini DCs by giving in-store teams intuitive tools that make it easy to execute and manage deliveries, orders, customer collections and returns – and never miss a sale."]
            [:a {:href "#", :class "btn btn-template wide shop-now"} "Shop Now"
             [:i {:class "icon-bag"} "           "]]]]]]]]
      )
    )

  )

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
  [:div {:id "exampleModal", :tabIndex "-1", :role "dialog", :aria-hidden "true", :class "modal fade overview"}
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

(defn header' []
  [:header {:class "header"} [navbar'] [menu']]
  )

(defn category-item-row[product]
  (let [{:keys [:thumbnail :ref :parent :name :price :description]} product]

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
         [:a {:href (str "#/product-details/" ref), :class "visit-product active"}
          [:i {:class "icon-search"}]"View"]
         ;[:a {:href "#", :data-toggle "modal", :data-target "#exampleModal", :class "quick-view"}
         ; [:i {:class "fa fa-arrows-alt"}]]
         ]]]
      [:div {:class "title"}
       ;[:small {:class "text-muted"} "Men Wear"] ;show category name here?
       [:a {:href (str "#/product-details/" ref)}
        [:h3 {:class "h6 text-uppercase no-margin-bottom"} name]]
       [:span {:class "price text-muted"} (currency price)]]]]

    ))

(defn product-list []

  (let [category-id (:category-id @(subscribe [:ecom/current-page-args]))  ;(get-in @app [:args :category-id])
        products @(subscribe [:ecom/products-by-supercategory category-id]) ;(ds/find-products-by-supercategory category-id)
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
       [:div {:class "row"}
          (map category-item-row products)
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

(defn order-row[row-item]
  (let [{:keys [:type :orderId :createdOn :totalPaidPrice :status]} row-item]
    ^{:key (str "odr-" orderId)}
    [:tr
     [:th orderId]
     [:td (date createdOn)]
     [:td (currency totalPaidPrice)]
     [:td
      [:span {:class "badge badge-info"} status]]
     [:td
      [:a {:href (str "#/order-details/" orderId), :class "btn btn-primary btn-sm"
           :on-click #(dispatch [:ecom/view-order-details orderId])} "View"]]]
    )
  )

(defn order-list[]
  (let [orders @(subscribe [:ecom/order-history])]
    [:div {:class "col-lg-8 col-xl-9 pl-lg-3"}
     [:table {:class "table table-hover table-responsive-md"}
      [:thead

       [:tr
        [:th "Order"]
        [:th "Date"]
        [:th "Total"]
        [:th "Status"]
        [:th "Action"]]]
      [:tbody
       (map order-row orders)
       ]]
     (let [paging @(subscribe [:ecom/order-paging])
           current-user @(subscribe [:ecom/current-user])
           customer-name (str (:firstname current-user) " " (:lastname current-user))]

        [:div {:class "container"}
         [:div {:class "row"}

                 [:div {:class "col-sm text-center"}
                  (when (:has-prev paging)
                    [:a {:class "fa fa-angle-double-left" :href "#order-history" :on-click #(dispatch [:ecom/load-orders {:query customer-name :start (:prev-page paging)}])} "  Previous"]
                    )
                  ]
                  [:div {:class "col-sm text-center"}
                   [:p (str "Viewing page: " (:current-page paging) " of " (:total-pages paging))]
                   ]
                 [:div {:class "col-sm text-center"}
                  (when (:has-next paging)
                    [:a {:class "fa" :href "#order-history" :on-click #(dispatch [:ecom/load-orders {:query customer-name :start (:next-page paging)}])} "Next  " [:span {:class "fa fa-angle-double-right"}]]
                    )
                  ]
          ]
        ]
       )

     ]
    )
  )


(defn order-item-row [item]
  (let [{:keys [:imageUrlRef :skuPrice :requestedQty :skuRef]} item
        qty-int (js/parseInt requestedQty)
        price-float (js/parseFloat skuPrice)
        product @(subscribe [:ecom/products-by-ref skuRef])
        ]
    ^{:key (str "odt-" (gensym))}
    [:div {:class "item"}
     [:div {:class "row d-flex align-items-center"}
      [:div {:class "col-6"}
       [:div {:class "d-flex align-items-center"}
        [:img {:src imageUrlRef, :alt "...", :class "img-fluid"}]
        [:div {:class "title"}
         [:a {:href "detail.html"}
          [:h6 (if (:description product) {:dangerouslySetInnerHTML {:__html (:description product)}} "Description unavailable" )]
          [:span {:class "text-muted"} (str "SKU: " skuRef)]]]]]
      [:div {:class "col-2"}
       [:span (currency skuPrice)]]
      [:div {:class "col-2"} requestedQty]
      [:div {:class "col-2 text-right"}
       [:span (currency (* price-float qty-int))]]]]
    )
  )

(defn order-details[]
  (println "Order details component...")
  (let [order-id (:order-id @(subscribe [:ecom/current-page-args]))
        order-details @(subscribe [:ecom/order-details])
        order-items (:items order-details)
        fulfillment-address (get-in order-details [:fulfilmentChoice :address])
        order-subtotal @(subscribe [:ecom/order-subtotal])
        hd? (= "HD" (:type order-details))
        cc? (= "CC" (:type order-details))
        shipping-cost (get-in order-details [:fulfilmentChoice :fulfilmentPrice])
        ]

    [:div {:class "container"}
    [:div  {:class "row" :style {"paddingTop" "15px"}}
  [:div {:class "col-lg-8"}

   [:div {:class "basket basket-customer-order"}
    [:div {:class "basket-holder"}
     [:div {:class "basket-header"}
      [:div {:class "row"}
       [:div {:class "col-6"} "Product"]
       [:div {:class "col-2"} "Price"]
       [:div {:class "col-2"} "Quantity"]
       [:div {:class "col-2 text-right"} "Total"]]]
     [:div {:class "basket-body"}

      (map order-item-row order-items)

      ]

     ]]


   ]
     [:div {:class "col-lg-4"}
      [:div {:class "item"}
       [:div {:class "row"}
        [:div {:class "offset-md-6 col-4"}
         [:strong "Order subtotal"]]
        [:div {:class "col-2 text-right"}
         [:strong (currency order-subtotal)]]]]
      [:div {:class "item"}
       [:div {:class "row"}
        [:div {:class "offset-md-6 col-4"}
         [:strong "Shipping and handling"]]
        [:div {:class "col-2 text-right"}
         [:strong (currency shipping-cost)]]]]
      [:div {:class "item"}
       [:div {:class "row"}
        [:div {:class "offset-md-6 col-4"}
         [:strong "Tax"]]
        [:div {:class "col-2 text-right"}
         [:strong "$0.00"]]]]
      [:div {:class "item"}
       [:div {:class "row"}
        [:div {:class "offset-md-6 col-4"}
         [:strong "Total"]]
        [:div {:class "col-2 text-right"}
         [:strong (currency (+ order-subtotal shipping-cost))]]]]
      ]
     ]
     [:div {:class "row addresses" :style {"paddingTop" "15px"}}
      [:div {:class "col-sm-6"}
       [:div {:class "block-header"}
        [:h6 {:class "text-uppercase"} (if hd? "Shipping address" "Store Pickup Address")]]
       [:div {:class "block-body"}
        [:p (when cc? (:companyName fulfillment-address)) (when hd? (:name fulfillment-address) )
         [:br](:street fulfillment-address)
         [:br](:city fulfillment-address)
         [:br](:state fulfillment-address)
         [:br](:postcode fulfillment-address)
         [:br](:country fulfillment-address)]
        ]
       ]

      ]
     ]
    )
  )

(defn details-header []
  (let [order-details @(subscribe [:ecom/order-details])]
    (println (str "Order details header data: " order-details))
      [:section {:class "hero hero-page gray-bg padding-small"}
       [:div {:class "container"}
        [:div {:class "row d-flex"}
         [:div {:class "col-lg-9 order-2 order-lg-1"}
          [:h1 "Order Details"]
          (when order-details
            [:p {:class "lead"} (str "Order Number: " (:orderId order-details) " Order Date: " (date (:createdOn order-details)))]
            )
          [:p {:class "lead"} (str "Order Status: " (or (:status order-details) "loading..."))]
          ]
         [:ul {:class "breadcrumb d-flex justify-content-start justify-content-lg-center col-lg-3 text-right order-1 order-lg-2"}
          [:li {:class "breadcrumb-item"}
           [:a {:href "index.html"} "Home"]]
          [:li {:class "breadcrumb-item active"} "Orders / Order Details"]]]]]
      )
    )

(defn section-header[title breadcrumb & [alternate]]
  (let [cart-item-count @(subscribe [:ecom/cart-items-count])
        cart-item-total @(subscribe [:ecom/cart-total])
        ]
  [:section {:class "hero hero-page gray-bg padding-small"}
   [:div {:class "container"}
    [:div {:class "row d-flex"}
     [:div {:class "col-lg-9 order-2 order-lg-1"}
      [:h1 title]
      (if alternate
        [:p {:class "lead text-muted"} alternate]
          [:span [:p {:class "lead text-muted"} (str "You currently have " cart-item-count " items in your shopping cart")]
          [:p {:class "lead text-muted"} (str "Cart total " (currency cart-item-total) )]]
        )

      ]
     [:ul {:class "breadcrumb d-flex justify-content-start justify-content-lg-center col-lg-3 text-right order-1 order-lg-2"}
      [:li {:class "breadcrumb-item"}
       [:a {:href "index.html"} "Home"]]
      [:li {:class "breadcrumb-item active"} breadcrumb]]]]]))

(defn cart-row[cart-item]
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
         [:div {:class "dec-btn" :on-click #(dispatch [:ecom/change-quantity key dec])} "-"]
         [:input {:type "text", :value quantity, :class "quantity-no"}]
         [:div {:class "inc-btn" :on-click #(dispatch [:ecom/change-quantity key inc])} "+"]]]]
      [:div {:class "col-2"}
       [:span (currency (* price quantity))]]
      [:div {:class "col-1 text-center"}
       [:i {:class "delete fa fa-trash" :on-click #(dispatch [:ecom/remove-cart-item key])}]]]]
    )
  )

(defn cart-contents []

  (let [cart-items @(subscribe [:ecom/cart-items])]
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
          (map cart-row cart-items)
          ]]]]
      [:div {:class "container"}
       [:div {:class "CTAs d-flex align-items-center justify-content-center justify-content-md-end flex-column flex-md-row"}
        ;[:a {:href "shop.html", :class "btn btn-template-outlined wide"} "Continue Shopping"]
        [:a {:href "#checkout-delivery", :class "btn btn-template wide"} "Checkout"]]]]
     ]
    )
    )
;)

(defn order-summary []
  (let [shipping @(subscribe [:ecom/shipping-cost])
        item-total @(subscribe [:ecom/cart-subtotal])
        ]
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

(defn set-value! [id value doc]
  (dispatch [:ecom/set-form-value doc id value])
  )

(defn maybe-keyword[val]
  (if (clojure.string/starts-with? val ":" )
    (keyword (clojure.string/replace-first val ":" ""))
    val
    )
  )

(defn text-input [id label name placeholder classes doc]
  (let [field-val @(subscribe [:ecom/forms doc (maybe-keyword id)])]
    [:div {:class (str "form-group " classes)}
     [:label {:for id :class "form-label"} label]
     [:input {:id    id :type "text" :name name :placeholder placeholder :class "form-control"
              :value field-val :on-change #(set-value! (maybe-keyword id) (-> % .-target .-value)  doc)}]]
    )
  )

(defn delivery []

  [:section {:class "checkout"}
   [:div {:class "container"}
    [:div {:class "row"}
     [:div {:class "col-md-6"}
      [:div {:class "tab-content"}

       [:div {:id "delivery", :class "active tab-block"}

        [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
         [:a {:href "#cart", :class "btn btn-template-outlined prev"}
          [:i {:class "fa fa-angle-left"}] "Back to Cart"]
         [:a {:href "#checkout-address", :class "btn btn-template " :on-click #(dispatch [:ecom/delivery-method :HD])}
          [:i {:class "fa"}] "Home Delivery"]
         [:a {:href "#checkout-store" :class "btn btn-template " :on-click #(dispatch [:ecom/delivery-method :CC])} "Click and Collect"
          [:i {:class "fa"}]]]
        ]]]
     [order-summary]
     ]]]

  )

(defn format-error [response]
  (str "Error returned from server " (:status response) " " (get-in response [:body :message]))
  )

(defn store-render []
      [:div {:class "container"}
               [:div {:class "CTAs d-flex  flex-column flex-lg-row"}
                [:a {:href "#checkout-delivery", :class "btn btn-template-outlined prev"}
                 [:i {:class "fa fa-angle-left"}] "Back to Delivery"]
                [:span {:style {"paddingLeft" "50px"}}]
                [:a {:href "#checkout-availability", :class "btn btn-template " :on-click #(dispatch [:ecom/check-fulfillment])} "Check Availability"
                 [:i {:class "fa"}]]
                ]
        [:div {:class "col-lg-8"}
          [:div {:id "store"}
           ]
         ]
        ]
     )

;TODO move to utils...
(defn- strip-colon [val]
  (if (clojure.string/starts-with? val ":")
    (clojure.string/replace-first val ":" "")
    val
    )
  )

(defn- symbol-key-to-string-key[source-map]
  (reduce-kv (fn [m k v]
               (assoc m (if (or (symbol? k) (clojure.string/starts-with? (str k) ":")) (strip-colon (str k)) k) v)
               ) {} source-map)
  )


(defn store-did-mount [this]
  (println "Store did mount!!")
  ;TODO check for presence of widget and change store vs reinit widget on every component render
  (let [cart-items @(subscribe [:ecom/cart-items])
        sku-qty (vec (map (fn[item] {"sku" (:ref item) "quantity" (:quantity item)}) cart-items))
        widget-config @(subscribe [:ecom/widget-config])
        main-config (symbol-key-to-string-key widget-config)
        current-user @(subscribe [:ecom/current-user])
        user-preference (:preferredAddress current-user)
        config (when main-config (assoc main-config "country" (symbol-key-to-string-key (get main-config "country"))) )
        ]
    (net/log (str "skus: " sku-qty))

    ; expecting a config like this if hardcoding a widget configuration is desired
    ;[config {"apiKey" "fluent-api-key"
    ;         "googleAPIKey" "google-maps-API-key"
    ;         "initialAddress" "Santa Monica, CA"
    ;         "country" {"name" "United States" "code" "US"}
    ;         }]

    (println (str "Using widget configuration: " config))
    (.initstore js/window
                (clj->js sku-qty)
                (clj->js (if user-preference (assoc config "initialAddress" user-preference) config)))
    )
  )

(defn store []
  (reagent/create-class {:reagent-render      #(store-render)
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

(defn cart-availability-row[ cart-item]
  (let [{:keys [:thumbnail :price :quantity :key :name :description :ref]} cart-item
        replacement @(subscribe [:ecom/replacement-sku])]
    ^{:key (str "cri-" key)}
    [:div {:class "item row d-flex align-items-center"}
     [:div {:class "col-6"}
      [:div {:class "d-flex align-items-center"}
       [:img {:src thumbnail, :alt "...", :class "img-fluid"}]
       [:div {:class "title"}
        [:a {:href "#"} ;show item details...
         [:h6 name]
         [:span {:class "text-muted"} (str "Sku: " ref)]]]]]
     [:div {:class "col-2"}
      [:span (currency price)]]
     [:div {:class "col-2"}
      [:span quantity]]
     [:div {:class "col-2"}
      [:span (currency (* price quantity))]]
     (when (= ref "SKU001")
       [:div {:class "col-6" :style {:paddingLeft "70px"}}[:div {:style {:backgroundColor "#b57983"} :on-click #(dispatch [:ecom/remove-cart-item key])} [:span "Out of Stock - click to remove"]]
       ]
       )
     (when (= ref "SKU703")
       [:div {:class "col-6" :style {:paddingLeft "70px"} :on-click #(dispatch [:ecom/substitute-item key replacement])} [:img {:src (:thumbnail replacement), :alt "..."}]
       [:div { :style {:backgroundColor "#ccbd88"}}
        [:span (str "Item unavailable - click to substitute with: " (:description replacement) "@ " (currency (:price replacement)))]
        ]]
       )
     (when (or (= ref "SKU001") (= ref "SKU703"))
       [:div {:class "delete"}
        [:i {:class "fa fa-trash-o" :on-click #(dispatch [:ecom/remove-cart-item key])}]]
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

;TODO EVENTS
;(defn add-to-cart[product-id app quantity]
;  (let [
;        product (ds/find-product-by-id product-id)
;        parent (ds/find-parent-product product-id)
;        cart (get :cart @app {})
;        ]
;    (println (str "add-to-cart:: adding " product-id " to cart"))
;    (let [cart-item (if parent
;                      parent
;                      product)
;                      ]
;      (println (str "cart item: " cart-item))
;      (dispatch [:ecom/add-to-cart cart-item quantity])
;      )
;    )
;  )

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

  (let [
        product-ref (:product-ref @(subscribe [:ecom/current-page-args]))
        base-product @(subscribe [:ecom/products-by-ref product-ref]) ; (ds/find-product-by-id product-id)
        is-variant? false                                   ;(:base base-product)
        variant-selector-id (:variant-selector base-product)
        variant-selectors @(subscribe [:ecom/variant-selectors])
        variant-selector (get variant-selectors variant-selector-id)
        valid-variants (get-in @app [:selected-variant product-ref])
        ;TODO fix variants... -> variant (merge base-product (first valid-variants))
        variant base-product
        cc-available @(subscribe [:ecom/available-for-cc])
        hd-available @(subscribe [:ecom/available-for-hd])
        ]

    (println (str "Pid " product-ref " product " base-product " variant-selector "
                  variant-selector " id -> " variant-selector-id))
    (println (str "Variant? " is-variant? " valid count: " (count valid-variants)))

    (dispatch [:ecom/check-inventory product-ref])

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
           [:li {:class "list-inline-item original"} (when (and (:original variant) (< (:price variant) (:original variant))) (currency (:original variant)))]]
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
             ;TODO was product-id... check this
             (make-selects product-ref variant-selector-id variant-selector app)
             ]
            )
          ]

         [:div {:class "fa"}
          [:p [:span {:class (if cc-available "fa-check-circle" "fa-ban")}]" Available for store pickup" ]
          [:p [:span {:class (if hd-available "fa-check-circle" "fa-ban")}]" Available for delivery"]
          ]

         [:ul {:class "CTAs list-inline"}
          [:li {:class "list-inline-item"}
           [:a {:href     "#", :class (if (and is-variant? (not= (count valid-variants) 1)) "btn btn-template wide disabled" "btn btn-template wide")
                :on-click (fn [e]
                            (. e preventDefault)
                            (println (str "Adding -> " (if is-variant? (:ref (first valid-variants)) product-ref)))
                            (dispatch [:ecom/add-to-cart base-product (int-input-value "pdp-qty-select")])
                            ;(add-to-cart (if is-variant? (:ref (first valid-variants)) product-ref) app (int-input-value "pdp-qty-select"))
                            )}
            [:i {:class "icon-cart"}] "Add to Cart"]]
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

(defn availability []

  ;{StoreId: "DR003", Line1: "Demo Store Melbourne", Line2: "300 Collingwood street", City: "MELBOURNE", County: "VIC", …}

;{StoreId DR004, Line1 Demo Store Sydney, Line2 200 Pitt Street, City SYDNEY, County NSW, Postcode 2000}
  (net/log "Availability::rendering component")
  (let [
        selected-store (js->clj (aget js/window "storeAddress" ))
        cart-total @(subscribe [:ecom/cart-total])
        cart-items @(subscribe [:ecom/cart-items])
        ]
    (println selected-store)

    [:div
    [:div {:style {:paddingLeft "40px"} :class " row"}
     [:span {:class "col-md-6"}
      [:h6 {:class "text-uppercase"} "Pickup Location"] [:span {:class "total"} (str (get selected-store "Line1" ) " -- " (get selected-store "Line2" ))] ]]
    [:section {:class "checkout"}
     ;TODO add this back into the FO call with a subscription?
     ;(when (:spinner-fo @app)
     ;  [:div [:img {:src "img/loading_128.GIF"}]]
     ;  )
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
             (doall (map cart-availability-row cart-items))
             ]]
           [:div {:class "total row"}
            [:span {:class "col-md-10 col-2"} "Total"]
            [:span {:class "col-md-2 col-10 text-primary"} (currency cart-total)]]]
          [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
           [:a {:href "#checkout-store", :class "btn btn-template-outlined wide prev"}
            [:i {:class "fa fa-angle-left"}]"Back to Choose Store"]
           [:a {:href "#checkout-payment", :class "btn btn-template wide next"} "Payment Method"
            [:i {:class "fa fa-angle-right"}]]]]]]
       [order-summary]
       ]]]]))

(defn checkout []

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
       [:a {:href "#checkout-address", :class "btn btn-template-outlined wide prev" :on-click #(dispatch [:ecom/use-saved-delivery-address])}
        [:i {:class "fa"}] "Use Saved Address"]
       ]

       [:div {:id "address", :class "active tab-block"}

        [:form {:action "#"}
         [:div {:class "row"}
          [text-input ":firstname" "First Name" "first-name" "Enter your first name" "col-md-6" :address]
          [text-input ":lastname" "Last Name" "last-name" "Enter your last name" "col-md-6" :address]
          [text-input ":email" "Email Address" "email" "Enter your email address" "col-md-6" :address]
          [text-input ":street" "Street" "street" "Enter your street address" "col-md-6" :address]
          [text-input ":city" "City" "city" "Your city" "col-md-3" :address]
          [text-input ":state" "State" "state" "Your state" "col-md-3" :address]
          [text-input ":zip" "Postal Code" "zip" "Your Postal Code" "col-md-3" :address]
          [text-input ":country" "Country" "country" "Your country" "col-md-3" :address]
          [text-input ":phone-number" "Phone Number" "phone-number" "Enter your phone number" "col-md-6" :address]
          ]]

        [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
         [:a {:href "#checkout-delivery", :class "btn btn-template-outlined wide prev"}
          [:i {:class "fa fa-angle-left"}] "Delivery Option"]
         [:a {:href "#checkout-shipping", :class "btn btn-template wide next"} "Choose shipping method"
          [:i {:class "fa fa-angle-right"}]]]]]]
     [order-summary]
     ]]])

(defn shipping[]

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
           [:input {:type "radio", :name "shippping", :id "option1", :class "radio-template" :on-click #(dispatch [:ecom/update-shipping :express-shipping])}]
           [:label {:for "option1"}
            [:strong "Next Day Shipping"]
            [:br]
            [:span {:class "label-description"} "Fastest Option"]]]
          [:div {:class "form-group col-md-6"}
           [:input {:type "radio", :name "shippping", :id "option2", :class "radio-template" :on-click #(dispatch [:ecom/update-shipping :standard-shipping])}]
           [:label {:for "option2"}
            [:strong "Standard Shipping"]
            [:br]
            [:span {:class "label-description"} "Two business days"]]
           ]
          ]]
        [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
         [:a {:href "#checkout-address", :class "btn btn-template-outlined wide prev"}
          [:i {:class "fa fa-angle-left"}]"Back to Address"]
         [:a {:href "#checkout-payment", :class "btn btn-template wide next"} "Choose payment method"
          [:i {:class "fa fa-angle-right"}]]]]]]
     [order-summary]

     ]]])

(defn payment[]
  (let [delivery @(subscribe [:ecom/delivery-type])
        hd (= delivery :HD) cc (= delivery :CC)]
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
               [:a {:href "#checkout-payment", :class "btn btn-template-outlined wide prev" :on-click #(dispatch [:ecom/use-saved-payment])}
                [:i {:class "fa"}] "Use Saved Payment"]
               ]

              [:form {:action "#" :autoComplete "false"}
               [:div {:class "row"}
                [text-input ":card-name" "Name on Card" "card-name" "Name on Card" "col-md-6" :payment]
                [text-input ":card-number" "Card Number" "card-number" "Card Number" "col-md-6" :payment]
                [text-input ":expiry-date" "Expiration Date" "expiry-date" "MM/YY" "col-md-4" :payment]
                [text-input ":card-cvv" "CVC/CVV" "card-cvv" "123" "col-md-4" :payment]
                [text-input ":card-zip" "Billing Postal Code" "card-zip" "Postal Code" "col-md-4" :payment]
                (when cc
                  [text-input ":phone-number" "Phone Number" "phone-number" "Enter your phone number" "col-md-6" :payment]
                  )
                (when cc
                  [text-input ":email" "Email Address" "email" "Enter your email address" "col-md-6" :payment]
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
             [:a {:href "#checkout-placeorder", :class "btn btn-template wide next" :on-click #(dispatch [:ecom/place-order])} "Place Order"
              [:i {:class "fa fa-angle-right"}]]
             )
           ]]]]
       [order-summary]
       ]]]
    )
  )

(defn cart-review-row[cart-item]
  (let [{:keys [:thumbnail :price :quantity :key :description :ref]} cart-item]
    ^{:key (str "cri-" key)}
    [:div {:class "item row d-flex align-items-center"}
     [:div {:class "col-6"}
      [:div {:class "d-flex align-items-center"}
       [:img {:src thumbnail, :alt "...", :class "img-fluid"}]
       [:div {:class "title"}
        [:a {:href "detail.html"}
         [:h6 {:dangerouslySetInnerHTML {:__html description}}]
         [:span {:class "text-muted"} (str "SKU: " ref)]]]]]
     [:div {:class "col-2"}
      [:span (currency price)]]
     [:div {:class "col-2"}
      [:span quantity]]
     [:div {:class "col-2"}
      [:span (currency (* price quantity))]]]
    ))

(defn cart-review []
  (let [cart-items @(subscribe [:ecom/cart-items])
        cart-total @(subscribe [:ecom/cart-total])]
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
           (map cart-review-row cart-items)
           ]]
         [:div {:class "total row"}
          [:span {:class "col-md-10 col-2"} "Total"]
          [:span {:class "col-md-2 col-10 text-primary"} (currency cart-total)]]]
        [:div {:class "CTAs d-flex justify-content-between flex-column flex-lg-row"}
         [:a {:href "#checkout-payment", :class "btn btn-template-outlined wide prev"}
          [:i {:class "fa fa-angle-left"}]"Back to payment method"]
         [:a {:href "#checkout-placeorder", :class "btn btn-template wide next" :on-click #(dispatch [:ecom/place-order])} "Place Order"
          [:i {:class "fa fa-angle-right"}]]]]]]
     [order-summary]
     ]]]))

(defn order-confirmation[]
  (let [order-id @(subscribe [:ecom/last-order-id])
        order-error @(subscribe [:ecom/order-error])]
    [:section {:class "padding-small"}
     [:div {:class "container"}
      [:div {:class "row about-item"}
       [:div {:class "col-lg-8 col-sm-9"}

        (if order-error
          [:h2 "There was a problem processing your order" [:p {:style {"color" "#b21129"}} order-error]]
          (if order-id
            [:h2 "Your order has been placed successfully"]
            [:h2 "Your order is processing"]
            )
          )
        ]
       [:div {:class "col-lg-4 col-sm-3 d-none d-sm-flex align-items-center"}
        [:div {:class "about-icon ml-lg-0"}
         [:img (when (nil? order-id) {:src "img/loading_128.GIF"})]]]]]]
    )
  )