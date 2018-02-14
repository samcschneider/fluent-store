(ns fr-api.data-source)

(def default-site "Maui Jim")

(def categories (atom [
                       {:id "c-001" :ref "KS-001" :name "Jewelery" :order 0}
                       {:id "c-002" :ref "KS-002" :name "Necklaces" :parent "KS-001" :order 1}
                       {:id "c-003" :ref "KS-003" :name "Earrings" :parent "KS-001" :order 2}
                       {:id "c-004" :ref "KS-004" :name "Bracelets" :parent "KS-001" :order 3}
                       {:id "c-010" :ref "AP-001" :name "Clothing" :order 10}
                       {:id "c-011" :ref "AP-002" :name "Shirts" :parent "AP-001" :order 1}

                 ;{:id "c-010.1" :ref "AP-0010-1" :name "Men's" :parent "AP-001" :order 1}
                      ;{:id "c-010.1" :ref "AP-0010-1" :name "Women's" :parent "AP-001" :order 2}
                      ; ;{:id "c-011" :ref "AP-002" :name "Shirts" :parent "AP-0010-1" :order 1}
                 ;{:id "c-050" :ref "Foo" :name "Electronics" :order 5}
                 ;{:id "c-060" :ref "Bar" :name "Brands" :order 4}
                       ]))

(defn find-category-by-parent[parent-id]
  (let [cats (filter #(= parent-id (:parent %)) @categories)]
    (sort-by :order cats)
    )
  )

;TODO divide multi level variants up and compose them...??
(def variant-selectors
  {"ap-001" [
             {:name "Color" :selector true :ref "cs1" :order 0}
             {:name "-- Select a Color --" :prompt true :order 1}
             {:name "Black" :ref "cblk1" :parent "c1" :order 3}
             {:name "Green" :ref "cgrn1" :parent "c1" :order 4}
             {:name "Red" :ref "cred1" :parent "c1" :order 5}
             {:name "Purple" :ref "cprp1" :parent "c1" :order 4}
             {:name "Size" :ref "ss1" :selector true :order 10}
             {:name "-- Select a Size --" :prompt true :order 11}
             {:name "Small" :parent "sc" :ref "ss1" :order 12}
             {:name "Medium" :parent "sc" :ref "sm1" :order 13}
             {:name "Large" :parent "sc" :ref "sl1" :order 14}
             {:name "X-Large" :parent "sc" :ref "sxl1" :order 15}
             ]

   "ksj-001" [
              {:name "Metal" :selector true :ref "ms1" :order 0}
              {:name "-- Choose Metal --" :prompt true :order 1}
              {:name "14k Yellow Gold" :ref "msyg" :parent "ms1" :order 2}
              {:name "14k Rose Gold" :ref "msrg" :parent "ms1" :order 3}
              ]

   "ksj-002" [
              {:name "Stone" :selector true :ref "jss1" :order 0}
              {:name "-- Choose Stone --" :prompt true :order 1}
              {:name "Bronze Veined Turquoise" :ref "jsbvt" :parent "jss1" :order 2}
              {:name "Slate Cat's Eye" :ref "jssce" :parent "jss1" :order 3}
              {:name "Iridescent Drusy" :ref "jsid" :parent "jss1" :order 4}
              {:name "Plum Drusy" :ref "jspd" :parent "jss1" :order 5}
              {:name "Cat's Eye Emerald" :ref "jscee" :parent "jss1" :order 6}
              ]

   "ksj-003" [
              {:name "Metal" :selector true :ref "ms2" :order 0}
              {:name "-- Choose Metal --" :prompt true :order 1}
              {:name "Silver" :ref "ms2s" :parent "ms2" :order 2}
              {:name "Gold" :ref "ms2g" :parent "ms2" :order 3}
              {:name "Rose Gold" :ref "ms2rg" :parent "ms2" :order 4}

              ]
   })


(def catalog (atom [
              ;base product
              {:id "p001" :ref "AP-SH01" :name "Fancy t-shirt"
               ;:description "Stylish t-shirt that is sure to impress your friends.<br>Made from imported silk.<br>Amazingly soft!"
               :description "  <strong>Frame:</strong> Rootbeer Fade<br>\n  <strong>Lens:</strong> Neutral Grey - Highest light reduction for rich color and contrast.<br>\n  <strong>Lens Material:</strong> MauiPure - The lightest weight choice for long days in the sun."
               :image "img/shirt-black.png" :thumbnail "img/shirt-black.png" :base true
               :price 40.0 :original 50.0 :variant-selector "ap-001" :supercategories #{"AP-001" "AP-002"}}

              ;color black
              ;size small
              {:id "p002" :sku "SKU002" :image "img/shirt-black.png" :thumbnail "img/shirt-black.png" :parent "AP-SH01" :variant-cats #{"ss1" "cblk1"}}
              ;size medium
              {:id "p003" :sku "SKU003" :image "img/shirt-black.png" :thumbnail "img/shirt-black.png" :parent "AP-SH01" :variant-cats #{"sm1" "cblk1"}}
              ;size large
              {:id "p004" :sku "SKU004" :image "img/shirt-black.png" :thumbnail "img/shirt-black.png" :parent "AP-SH01" :variant-cats #{"sl1" "cblk1"}}
              ;size xl
              {:id "p005" :sku "SKU005" :image "img/shirt-black.png" :thumbnail "img/shirt-black.png" :parent "AP-SH01" :variant-cats #{"sxl1" "cblk1"}}

              ;color green
              ;size small
              {:id "p006" :sku "SKU006" :image "img/shirt-green.png" :thumbnail "img/shirt-green.png" :parent "AP-SH01" :variant-cats #{"ss1" "cgrn1"}}
              ;size medium
              {:id "p007" :sku "SKU007" :image "img/shirt-green.png" :thumbnail "img/shirt-green.png" :parent "AP-SH01" :variant-cats #{"sm1" "cgrn1"}}
              ;size large
              {:id "p008" :sku "SKU008" :image "img/shirt-green.png" :thumbnail "img/shirt-green.png" :parent "AP-SH01" :variant-cats #{"sl1" "cgrn1"}}
              ;size xl
              {:id "p009" :sku "SKU009" :image "img/shirt-green.png" :thumbnail "img/shirt-green.png" :parent "AP-SH01" :variant-cats #{"sxl1" "cgrn1"}}

              ;red and purple will just be available in two sizes each
              ;color red
              ;size small
              {:id "p010" :name "Fancy t-shirt (premium dye)" :sku "SKU010" :image "img/shirt-red.png" :thumbnail "img/shirt-red.png" :parent "AP-SH01" :price 70.0 :original 90.0 :variant-cats #{"ss1" "cred1"}}
              ;size large
              {:id "p011" :name "Fancy t-shirt (premium dye)" :sku "SKU011" :image "img/shirt-red.png" :thumbnail "img/shirt-red.png" :parent "AP-SH01" :price 70.0 :original 90.0 :variant-cats #{"sl1" "cred1"}}

              ;red and purple will just be available in two sizes each
              ;color purple
              ;size medium
              {:id "p012" :sku "SKU012" :image "img/shirt.png" :thumbnail "img/shirt.png" :parent "AP-SH01" :variant-cats #{"sm1" "cprp1"}}
              ;size xl
              {:id "p013" :sku "SKU013" :image "img/shirt.png" :thumbnail "img/shirt.png" :parent "AP-SH01" :variant-cats #{"sxl1" "cprp1"}}
              ])

  )

(defn find-product-by-id[product-id]
  (first (filter #(= product-id (:id %)) @catalog))
  )

(defn find-product-by-ref[product-ref]
  (first (filter #(= product-ref (:ref %)) @catalog))
  )

(defn find-category-by-id[category-id]
  (first (filter #(= category-id (:id %)) @categories))
  )

(defn find-products-by-supercategory[category-id]
  (when-let [category (find-category-by-id category-id)]
    (filter #(contains? (:supercategories %) (:ref category)) @catalog)
    )
  )

(defn find-parent-product [product-id]
  (when-let [product (find-product-by-id product-id)]
    (first (filter #(= (:parent product) (:ref %)) @catalog))
    )
  )

;;Some sample cart items
(def c1 {:key 0 :name "Amazing purple shirt" :description "Amazing purple shirt" :sku "SKU001" :image "" :thumbnail "img/shirt.png" :price 40.00 :quantity 3})
(def c2 {:key 1 :name "Cool black shirt" :description "Cool black shirt" :sku "SKU002" :image "" :thumbnail "img/shirt-black.png" :price 12.00 :quantity 2})
(def c3 {:key 2 :name "Stylish black shirt" :description "Stylish black shirt" :sku "SKU003" :image "" :thumbnail "img/shirt-black.png" :price 25.00 :quantity 4})

(def replacement {:name "Braeburn Apples" :description "Braeburn Apples" :sku "SKU902" :thumbnail "img/apple-braeburn.jpg" :price 19.00})

(def saved-address {"lastname" "Schneider" "country" "USA" "city" "Los Angeles" "email" "sam.schneider@fluentcommerce.com" "state" "CA" "street" "200 Main Street" "firstname" "Sam" "zip" "90275" "phone-number" "+1555-444-1212"})
(def saved-payment {"card-name" "Samuel Schneider" "card-number" "4111 1111 1111 1111" "expiry-date" "12/19" "card-cvv" "333" "card-zip" "90275" "email" "sam.schneider@fluentcommerce.com" "phone-number" "+1555-444-1212"})



