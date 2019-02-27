(ns carbon-calc.core
    (:require
      [reagent.core :as r]
      [carbon-calc.util.table :as table]
      [goog.string :as gstring]
      [goog.string.format]))
;; -------------------------
;; Views


(def assumptions (r/atom {:allowance-floor-price 16
                          :offsets-cap 8
                          :offset-benefiting-oregon 50
                          :offset-discount 20}))

(def exempts (r/atom {:ele-uti false
                      :dir-ele-ser-sup false
                      :oth-ele true
                      :nat-gas-uti false
                      :nat-gas-mar false
                      :dir-reg-man-poi-sou false
                      :oth-poi-sou false
                      :non-roa false
                      :on-roa false}))

(def allowances (r/atom {:ele-uti 4
                         :dir-ele-ser-sup 0
                         :oth-ele 0
                         :nat-gas-uti 20
                         :nat-gas-mar 0
                         :dir-reg-man-poi-sou 100
                         :oth-poi-sou 0
                         :non-roa 0
                         :on-roa 40}))

(def totals (r/atom nil))

(def assumed-emissions {:ele-uti 13.33
                        :dir-ele-ser-sup 0.72
                        :oth-ele 3.46
                        :nat-gas-uti 4.10
                        :nat-gas-mar 1.02
                        :dir-reg-man-poi-sou 2.54
                        :oth-poi-sou 0.99
                        :non-roa 4.25
                        :on-roa 20.73})

(def constants (assoc-in {:assumed-emissions assumed-emissions} [:assumed-emissions :total]
                         (reduce + (vals assumed-emissions))))


(defn assumptions-slider [param value min max]
  [:input {:type "range" :value value :min min :max max
           :style {:width "100%"}
           :on-change (fn [e]
                        (swap! assumptions assoc param (.. e -target -value))
                        (when (not= param :sum)
                          (swap! totals assoc :sum nil)))}])

(defn assumptions-table []
  (let [{:keys [:allowance-floor-price :offsets-cap :offset-benefiting-oregon :offset-discount] :as assumptions} @assumptions]
    [:fieldset
     [:legend [:h3 "Assumptions Table"]]
     (table/to-table1d (list ["Allowance Reserve Floor Price, $/mmCO2e1   ($14.53 in 2018, projected to be $16.77 in 2021 in 2018 dollars)"
                              [assumptions-slider :allowance-floor-price allowance-floor-price  0 100]
                              (str "$" allowance-floor-price ".00")]
                             ["Offsets Cap (relative to each entities' GHG emissions)"
                              [assumptions-slider :offsets-cap offsets-cap 0 100]
                              (str offsets-cap "%")]
                             ["Offsets % that must directly benefit Oregon"
                              [assumptions-slider :offset-benefiting-oregon offset-benefiting-oregon 0 100]
                              (str offset-benefiting-oregon "%")]
                             ["Offsets Price Discount Relative to Allowance Price"
                              [assumptions-slider :offset-discount offset-discount 0 100]
                              (str offset-discount "%")])
                       [0 "assumption" 1 "slider" 2 "value"]
                       {:show-head? false})]))


(defn category-slider [param]
  [:div
   [:div (str (@allowances param) "%")]
   [:input {:type "range" :value (@allowances param) :min 0 :max 100
            :style {:width "100%"}
            :on-change (fn [e]
                         (swap! allowances assoc param (.. e -target -value))
                         (when (not= param :sum)
                           (swap! totals assoc :sum nil)))}]])

(defn- switch-exempt [category]
  (fn [e] (swap! exempts update category not)))

(defn category-checkbox [param]
  [:div
   [:div (str (@exempts param))]
   [:input {:type :checkbox
            :value (str (@exempts param))
            :checked (when (@exempts param) "checked")
            :on-click (switch-exempt param)}]])

(defn comma-money [cash]
  (->> (str cash)
    reverse
    (partition-all 3)
    (map (partial apply str))
    (clojure.string/join ",")
    reverse
    (apply str)))


(defn round-up [num]
  (let [i (int num)]
    (if (> (- num i) 0.5)
      (inc i)
      i)))

(defn max-pot-revenue [param]
  (if (@exempts param) 0
    (round-up
      (* (param (:assumed-emissions constants))
         1000000
         (:allowance-floor-price @assumptions)))))

(defn pot-rein-rev-lost [param]
  (if (and (not (@exempts param))
           (> (@assumptions :offset-discount) 0)
           (< (@allowances param) 100))
    (* (param (:assumed-emissions constants))
       10000
       (min (- 100 (@allowances param))
            (@assumptions :offsets-cap))
       (:allowance-floor-price @assumptions))
    0))

(defn rein-rev-lost [param]
  (if (@exempts param) 0
      (* (param (:assumed-emissions constants))
         (param @allowances)
         (:allowance-floor-price @assumptions)
         10000)))

(def all-cat-keys #{:ele-uti :dir-ele-ser-sup :oth-ele :nat-gas-uti :nat-gas-mar :dir-reg-man-poi-sou :oth-poi-sou :non-roa :on-roa})

(defn total-cash-minus-special []
  (reduce + (map #(- (max-pot-revenue %) (+ (pot-rein-rev-lost %) (rein-rev-lost %)))
                 (disj all-cat-keys :on-roa))))

(defn to-transport-decarb-account []
  (- (max-pot-revenue :on-roa) (pot-rein-rev-lost :on-roa)))

(defn big-total []
  (+ (total-cash-minus-special) (to-transport-decarb-account)))



(defn category-row [name cat-key]
  (let [total (- (max-pot-revenue cat-key)
                 (+ (pot-rein-rev-lost cat-key) (rein-rev-lost cat-key)))]
    [name
     (cat-key (:assumed-emissions constants))
     (str (int (* 100 (/ (cat-key (:assumed-emissions constants))
                         (:total (:assumed-emissions constants))))) "%")
     [category-checkbox cat-key]
     [category-slider cat-key]
     (comma-money (max-pot-revenue cat-key))
     (comma-money (pot-rein-rev-lost cat-key))
     (comma-money (rein-rev-lost cat-key))
     (comma-money total)
     0
     (comma-money total)
     (str (int (* 100 (/ total (big-total)))) "%")]))



(defn totals-row []
    ["Total Covered Emissions4"
     (:total (:assumed-emissions constants))
     "100%"
     ""
     ""
     (comma-money (reduce + (map max-pot-revenue all-cat-keys)))
     (comma-money (reduce + (map pot-rein-rev-lost all-cat-keys)))
     (comma-money (reduce + (map rein-rev-lost all-cat-keys)))
     (comma-money (total-cash-minus-special))
     (comma-money  (to-transport-decarb-account))
     (comma-money (big-total))
     "100%"])

(defn category-table []
  (let [a 1]
    [:fieldset
     [:legend [:h3 "Category Table"]]
     (table/to-table1d (list (category-row "Electric Utilities" :ele-uti)
                             (category-row "Direct Electricity Service Suppliers" :dir-ele-ser-sup)
                             (category-row "Other Electricity (exported out of state)" :oth-ele)
                             (category-row "Natural Gas Utilities" :nat-gas-uti)
                             (category-row "Natural Gas Marketers4" :nat-gas-mar)
                             (category-row "Directly Regulated Manufacturing Point Sources" :dir-reg-man-poi-sou)
                             (category-row "Other Points Sources (i.e., landfills, gas compressors)" :oth-poi-sou)
                             (category-row "Non-road Fuels" :non-roa)
                             (-> (category-row "On-road Fuels" :on-roa)
                               (assoc 9 (comma-money (to-transport-decarb-account)))
                               (assoc 8 0))
                             (totals-row))
                       [0 "Emissions Covered by the Cap"
                        1 "2021 Assumed Emissions3 (in Million MTCo2r)"
                        2 "% of regulated emissions"
                        3 "Exempted from being Covered?"
                        4 "Free allowances allocated"
                        5 "Maximum Potential Revenue"
                        6 "Potential Reinvestment Revenues lost to Offsets"
                        7 "Reinvestment Revenues lost to Free Allowances"
                        8 "Proceeds to Climate Investment Fund"
                        9 "Proceeds to Transportation Decarbonization Account"
                        10 "Total Net Reinvestment Proceeds"
                        11 "% of total Proceeds"]
                       {:show-head? true})]))

(defn home-page []
  [:div
   [:h2 "Oregon Climate Action Plan (HB 2020)_Reinvestment Proceeds Estimates"]
   [assumptions-table]
   [category-table]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
