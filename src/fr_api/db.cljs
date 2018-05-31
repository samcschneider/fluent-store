(ns fr-api.db
  (:require [fr-api.data-source :as ds])
  )

(def cart-item {:description
                                    "<strong>Frame:</strong> Matte Brown Wood Grain<br><strong>Lens:</strong> HCL® Bronze - Versatile in changing conditions with a warm tint.<br><strong>Lens Material:</strong> SuperThin Glass - Perfect when clarity is your highest priority.",
                   :key 3,
                   :ref "MJS-008",
                   :name "Kahi",
                   :supercategories #{"MJ-001" "MJ-004"},
                   :sku "MJS-008",
                   :thumbnail
                                    "https://mauijim.scene7.com/is/image/mauijim/H736-25W-01?$config1098$",
                   :id "p14578",
                   :image
                                    "https://mauijim.scene7.com/is/image/mauijim/H736-25W-01?$config1098$",
                   :quantity 1,
                   :price 249})
(def default-db

  {:cart-items (sorted-map)
   :sites      []
   :variant-selectors ds/variant-selectors
   :saved-address ds/saved-address ;TODO move to config
   :saved-payment ds/saved-payment ;TODO move to config
   :config {:name "Local" :categories @ds/categories :products @ds/catalog}
   }
  )