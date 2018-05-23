(ns fr-api.data-source)

(def default-site "Maui Jim")

(def categories (atom {
                       "c-001" {:id "c-001" :ref "KS-001" :name "Jewelery" :order 0}
                       "c-002" {:id "c-002" :ref "KS-002" :name "Necklaces" :parent "KS-001" :order 1}
                       "c-003" {:id "c-003" :ref "KS-003" :name "Earrings" :parent "KS-001" :order 2}
                       "c-004" {:id "c-004" :ref "KS-004" :name "Bracelets" :parent "KS-001" :order 3}
                       "c-005" {:id "c-010" :ref "AP-001" :name "Clothing" :order 10}
                       "c-006" {:id "c-011" :ref "AP-002" :name "Shirts" :parent "AP-001" :order 1}
                       }))

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


              {:id "p022" :ref "GP-001" :name "Organic Apples" :description "Tasty Braeburn apple"
               :image "img/apple-braeburn.jpg" :thumbnail "img/apple-braeburn.jpg" :sku "SKU902"
               :price 2.0 :original 2.99 }

              ;KS :: Jewelery :: Necklaces

              {:id "pksj001" :ref "JN-001" :name "Alina Choker Necklace In Multi Gemstone Mix And 14k Yellow Gold" :description "Featuring an adjustable closure and diamond accent, the Alina Choker Necklace in Neutral Gemstone Mix and 14k Rose Gold shines with stunning semi-precious gemstones. Wear this artful piece to perfect your everyday look or layer with other styles for a simple statement."
               :image "img/ks/kendra-scott-alina-choker-necklace-in-yellow-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-alina-choker-necklace-in-yellow-gold_00_default_lg.jpg" :base true
               :price 795.0  :variant-selector "ksj-001" :supercategories #{"KS-001" "KS-002"}}

              {:id "pksj002" :ref "JN-001-V1" :sku "KSJAN001" :name "Alina Choker Necklace In Multi Gemstone Mix And 14k Yellow Gold" :price 795.0 :image "img/ks/kendra-scott-alina-choker-necklace-in-yellow-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-alina-choker-necklace-in-yellow-gold_00_default_lg.jpg" :parent "JN-001" :variant-cats #{"msyg"} :default true}
              {:id "pksj003" :ref "JN-001-V2" :sku "KSJAN002" :name "Alina Choker Necklace In Neutral Gemstone Mix And 14k Rose Gold" :price 750.0  :image "img/ks/kendra-scott-alina-choker-necklace-in-rose-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-alina-choker-necklace-in-rose-gold_00_default_lg.jpg" :parent "JN-001" :variant-cats #{"msrg"}}

              {:id "pksj005" :ref "JN-002" :name "Elisa Pendant Necklace In Iridescent Drusy" :description "A dainty stone and delicate metallic chain combine to create the Elisa Pendant Necklace in Iridescent Drusy, your new favorite wear-anywhere accessory."
               :image "img/ks/iridescent-drusy-842177092088_01_default_lg.jpg" :thumbnail "img/ks/iridescent-drusy-842177092088_01_default_lg.jpg" :base true
               :price 65.00  :variant-selector "ksj-002" :supercategories #{"KS-001" "KS-002"}}

              {:id "pksj006" :ref "JN-002-V1" :sku "KSJEP001" :name "Elisa Pendant Necklace In Iridescent Drusy" :description "A dainty stone and delicate metallic chain combine to create the Elisa Pendant Necklace in Iridescent Drusy, your new favorite wear-anywhere accessory." :price 65.0 :image "img/ks/iridescent-drusy-842177092088_01_default_lg.jpg" :thumbnail "img/ks/iridescent-drusy-842177092088_01_default_lg.jpg" :parent "JN-002" :variant-cats #{"jsid"} :default true :base false}
              {:id "pksj007" :ref "JN-002-V2" :sku "KSJEP002" :name "Elisa Pendant Necklace In Iridescent Plum" :description "A dainty stone and delicate metallic chain combine to create the Elisa Pendant Necklace in Plum Drusy, your new favorite wear-anywhere accessory." :price 65.0 :image "img/ks/kendra-scott-elisa-gold-pendant-necklace-in-plum-drusy_01_default_lg.jpg" :thumbnail "img/ks/kendra-scott-elisa-gold-pendant-necklace-in-plum-drusy_01_default_lg.jpg" :parent "JN-002" :variant-cats #{"jspd"} :base false}
              {:id "pksj008" :ref "JN-002-V3" :sku "KSJEP003" :name "Elisa Pendant Necklace In Slate" :description "A dainty stone and delicate metallic chain combine to create the Elisa Pendant Necklace in Slate, your new favorite wear-anywhere accessory." :price 50.0 :image "img/ks/slate-842177114513_01_default_lg.jpg" :thumbnail "img/ks/slate-842177114513_01_default_lg.jpg" :parent "JN-002" :variant-cats #{"jssce"} :base false}
              {:id "pksj009" :ref "JN-002-V4" :sku "KSJEP004" :name "Elisa Pendant Necklace In Cat's Eye Emerald" :description "A dainty stone and delicate metallic chain combine to create the Elisa Pendant Necklace in Cat's Eye Emerald, your new favorite wear-anywhere accessory." :price 60.0 :image "img/ks/kendra-scott-elisa-gold-pendant-necklace-in-emerald_01_default_lg.jpg" :thumbnail "img/ks/kendra-scott-elisa-gold-pendant-necklace-in-emerald_01_default_lg.jpg" :parent "JN-002" :variant-cats #{"jscee"} :base false}
              {:id "pksj010" :ref "JN-002-V5" :sku "KSJEP005" :name "Elisa Pendant Necklace In Bronze Veined Turquoise" :description "A dainty stone and delicate metallic chain combine to create the Elisa Pendant Necklace in Bronze Veined Turquoise, your new favorite wear-anywhere accessory." :price 65.0 :image "img/ks/veined-turquoise-842177147900_01_default_lg.jpg" :thumbnail "img/ks/veined-turquoise-842177147900_01_default_lg.jpg" :parent "JN-002" :variant-cats #{"jsbvt"} :base false}

              {:id "pksj015" :ref "JN-003" :name "Arlo Lariat Necklace In Silver" :description "With cascades of crystals and fringed ends, the Arlo Lariat Necklace in Silver is guaranteed to make a statement."
               :image "img/ks/kendra-scott-arlo-lariat-necklace-in-silver_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-arlo-lariat-necklace-in-silver_00_default_lg.jpg" :base true
               :price 195.00  :variant-selector "ksj-003" :supercategories #{"KS-001" "KS-002"}}

              {:id "pksj016" :ref "JN-003-V1" :sku "KSJAL001" :name "Arlo Lariat Necklace In Silver" :description "With cascades of crystals and fringed ends, the Arlo Lariat Necklace in Silver is guaranteed to make a statement." :image "img/ks/kendra-scott-arlo-lariat-necklace-in-silver_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-arlo-lariat-necklace-in-silver_00_default_lg.jpg" :parent "JN-003" :variant-cats #{"ms2s"} :default true :base false}
              {:id "pksj017" :ref "JN-003-V2" :sku "KSJAL002" :name "Arlo Lariat Necklace In Gold" :description "With cascades of crystals and fringed ends, the Arlo Lariat Necklace in Gold is guaranteed to make a statement." :image "img/ks/kendra-scott-arlo-lariat-necklace-in-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-arlo-lariat-necklace-in-gold_00_default_lg.jpg" :parent "JN-003" :variant-cats #{"ms2g"} :default true :base false}
              {:id "pksj018" :ref "JN-003-V3" :sku "KSJAL003" :name "Arlo Lariat Necklace In Rose Gold" :description "With cascades of crystals and fringed ends, the Arlo Lariat Necklace in Rose Gold is guaranteed to make a statement." :image "img/ks/kendra-scott-arlo-lariat-necklace-in-rose-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-arlo-lariat-necklace-in-rose-gold_00_default_lg.jpg" :parent "JN-003" :variant-cats #{"ms2rg"} :default true :base false}

              ;KS :: Jewelery :: Earrings

              {:id "pksj020"  :ref "JE-001" :sku "KSJTS001" :name "Taylor Stud Earrings In Blue Drusy" :description "Modern elegance radiates from the Taylor Stud Earrings in Blue Drusy, a fool-proof accessory to elevate any look."
               :image "img/ks/kendra-scott-taylor-gold-stud-earrings-in-blue-drusy_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-taylor-gold-stud-earrings-in-blue-drusy_00_default_lg.jpg"
               :price 80.00 :supercategories #{"KS-001" "KS-003"}}

              {:id "pksj022" :ref "JE-002" :name "Alice Statement Earrings In Gold" :description "For a stunning update to a simple silhouette, try the Alice Statement Earrings in Gold for its elongated shape and sophisticated studs."
               :image "img/ks/kendra-scott-alice-statement-earrings-in-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-alice-statement-earrings-in-gold_00_default_lg.jpg" :base true
               :price 85.00  :variant-selector "ksj-003" :supercategories #{"KS-001" "KS-003"}}

              {:id "pksj023" :ref "JE-002-V1" :sku "KSJAS001" :name "Alice Statement Earrings In Antique Silver" :description "For a stunning update to a simple silhouette, try the Alice Statement Earrings in Antique Silver for its elongated shape and sophisticated studs." :image "img/ks/kendra-scott-alice-statement-earrings-in-antique-silver_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-alice-statement-earrings-in-antique-silver_00_default_lg.jpg" :parent "JE-002" :variant-cats #{"ms2s"} :default true :base false}
              {:id "pksj024" :ref "JE-002-V2" :sku "KSJAS002" :name "Alice Statement Earrings In Gold" :description "For a stunning update to a simple silhouette, try the Alice Statement Earrings in Gold for its elongated shape and sophisticated studs." :image "img/ks/kendra-scott-alice-statement-earrings-in-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-alice-statement-earrings-in-gold_00_default_lg.jpg" :parent "JE-002" :variant-cats #{"ms2g"} :base false}
              {:id "pksj025" :ref "JE-002-V3" :sku "KSJAS003" :name "Alice Statement Earrings In Rose Gold" :description "For a stunning update to a simple silhouette, try the Alice Statement Earrings in Rose Gold for its elongated shape and sophisticated studs." :image "img/ks/kendra-scott-alice-statement-earrings-in-rose-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-alice-statement-earrings-in-rose-gold_00_default_lg.jpg" :parent "JE-002" :variant-cats #{"ms2rg"} :base false}

              {:id "pksj027" :ref "JE-003" :name "Olympia Statement Earrings In Gold" :description "Cascades of stunning crystals make the Olympia Statement Earrings in Gold your shimmering staple for every occasion."
               :image "img/ks/kendra-scott-olympia-tassel-statement-earrings-in-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-olympia-tassel-statement-earrings-in-gold_00_default_lg.jpg" :base true
               :price 150.00  :variant-selector "ksj-003" :supercategories #{"KS-001" "KS-003"}}

              {:id "pksj028" :ref "JE-003-V1" :sku "KSJOS001" :name "Olympia Statement Earrings In Gold" :description "Cascades of stunning crystals make the Olympia Statement Earrings in Gold your shimmering staple for every occasion." :image "img/ks/kendra-scott-olympia-tassel-statement-earrings-in-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-olympia-tassel-statement-earrings-in-gold_00_default_lg.jpg" :parent "JE-003" :variant-cats #{"ms2g"} :default true :base false}
              {:id "pksj029" :ref "JE-003-V2" :sku "KSJOS002" :name "Olympia Statement Earrings In Silver" :description "Cascades of stunning crystals make the Olympia Statement Earrings in Silver your shimmering staple for every occasion." :image "img/ks/kendra-scott-olympia-tassel-statement-earrings-in-silver_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-olympia-tassel-statement-earrings-in-silver_00_default_lg.jpg" :parent "JE-003" :variant-cats #{"ms2s"} :base false}
              {:id "pksj030" :rev "JE-003-V3" :sku "KSJOS003" :name "Olympia Statement Earrings In Rose Gold" :description "Cascades of stunning crystals make the Olympia Statement Earrings in Rose Gold your shimmering staple for every occasion." :image "img/ks/kendra-scott-olympia-tassel-statement-earrings-in-rose-gold_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-olympia-tassel-statement-earrings-in-rose-gold_00_default_lg.jpg" :parent "JE-003" :variant-cats #{"ms2rg"} :base false}

              ;KS :: Jewelery :: Bracelets

              {:id "pksj033" :ref "JB-001" :name "Deb Adjustable Chain Bracelet In Gold" :description "The romantic gold details and delicate stones featured in our Deb Adjustable Bracelet add visual interest and classic shine to your favorite bracelet stack."
               :image "img/ks/deb_gold_842177153659_00_default_lg.jpg" :thumbnail "img/ks/deb_gold_842177153659_00_default_lg.jpg" :base true
               :price 60.00  :variant-selector "ksj-003" :supercategories #{"KS-001" "KS-004"}}

              {:id "pksj034" :ref "JB-001-V1" :sku "KSJDB001" :name "Deb Adjustable Chain Bracelet In Gold" :description "The romantic gold details and delicate stones featured in our Deb Adjustable Bracelet add visual interest and classic shine to your favorite bracelet stack." :image "img/ks/deb_gold_842177153659_00_default_lg.jpg" :thumbnail "img/ks/deb_gold_842177153659_00_default_lg.jpg" :parent "JB-001" :variant-cats #{"ms2g"} :default true :base false}
              {:id "pksj035" :ref "JB-001-V2" :sku "KSJDB002" :name "Deb Adjustable Chain Bracelet In Silver" :description "The romantic silver details and delicate stones featured in our Deb Adjustable Bracelet add visual interest and classic shine to your favorite bracelet stack." :image "img/ks/deb_silver_842177153673_00_default_lg.jpg" :thumbnail "img/ks/deb_silver_842177153673_00_default_lg.jpg" :parent "JB-001" :variant-cats #{"ms2s"} :base false}
              {:id "pksj036" :ref "JB-001-V3" :sku "KSJDB003" :name "Deb Adjustable Chain Bracelet In Rose Gold" :description "The romantic rose gold details and delicate stones featured in our Deb Adjustable Bracelet add visual interest and classic shine to your favorite bracelet stack." :image "img/ks/deb_rose_gold_842177153666_00_default_lg.jpg" :thumbnail "img/ks/deb_rose_gold_842177153666_00_default_lg.jpg" :parent "JB-001" :variant-cats #{"ms2rg"} :base false}

              {:id "pksj038" :ref "JB-002" :sku "KSJTB001" :name "Teddy Pinch Bracelet In Blue Drusy" :description "Two dainty stones bookend the Teddy Pinch Bracelet in Blue Drusy for an elegant addition to any stack."
               :image "img/ks/kendra-scott-teddy-gold-pinch-bracelet-in-blue-drusy_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-teddy-gold-pinch-bracelet-in-blue-drusy_00_default_lg.jpg"
               :price 80.00 :supercategories #{"KS-001" "KS-004"}}

              {:id "pksj040" :ref "JB-003" :name "Delphine Pinch Bracelet Set In Gold" :description "Adjust to a size that suits you with our Delphine Pinch Bracelet Set in Gold, featuring crown inspired details and stunning white CZ accents."
               :image "img/ks/kendra-scott-delphine-gold-pinch-bracelet-set_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-delphine-gold-pinch-bracelet-set_00_default_lg.jpg" :base true
               :price 120.00  :variant-selector "ksj-003" :supercategories #{"KS-001" "KS-004"}}

              {:id "pksj041" :ref "JB-003-V1" :sku "KSJDP001" :name "Deb Adjustable Chain Bracelet In Gold" :description "Adjust to a size that suits you with our Delphine Pinch Bracelet Set in Gold, featuring crown inspired details and stunning white CZ accents." :image "img/ks/kendra-scott-delphine-gold-pinch-bracelet-set_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-delphine-gold-pinch-bracelet-set_00_default_lg.jpg" :parent "JB-003" :variant-cats #{"ms2g"} :default true :base false}
              {:id "pksj042" :ref "JB-003-V2" :sku "KSJDP002" :name "Delphine Pinch Bracelet Set In Antique Silver" :description "Adjust to a size that suits you with our Delphine Pinch Bracelet Set in Antique Sliver, featuring crown inspired details and stunning white CZ accents." :image "img/ks/kendra-scott-delphine-antique-silver-pinch-bracelet-set_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-delphine-antique-silver-pinch-bracelet-set_00_default_lg.jpg" :parent "JB-003" :variant-cats #{"ms2s"} :base false}
              {:id "pksj043" :ref "JB-003-V3" :sku "KSJDP003" :name "Delphine Pinch Bracelet Set In Rose Gold" :description "Adjust to a size that suits you with our Delphine Pinch Bracelet Set in Rose Gold, featuring crown inspired details and stunning white CZ accents." :image "img/ks/kendra-scott-delphine-rose-gold-pinch-bracelet-set_00_default_lg.jpg" :thumbnail "img/ks/kendra-scott-delphine-rose-gold-pinch-bracelet-set_00_default_lg.jpg" :parent "JB-003" :variant-cats #{"ms2rg"} :base false}

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



