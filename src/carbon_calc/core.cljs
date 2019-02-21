(ns carbon-calc.core
    (:require
      [reagent.core :as r]
      [ carbon-calc.util.table :as table]))

;; -------------------------
;; Views

(def bmi-data (r/atom {:height 180 :weight 80}))

(defn calc-bmi []
  (let [{:keys [height weight bmi] :as data} @bmi-data
        h (/ height 100)]
    (if (nil? bmi)
      (assoc data :bmi (/ weight (* h h)))
      (assoc data :weight (* bmi h h)))))

(defn slider [param value min max]
  [:input {:type "range" :value value :min min :max max
           :style {:width "100%"}
           :on-change (fn [e]
                        (swap! bmi-data assoc param (.. e -target -value))
                        (when (not= param :bmi)
                          (swap! bmi-data assoc :bmi nil)))}])

(defn bmi-component []
  (let [{:keys [weight height bmi]} (calc-bmi)
        [color diagnose] (cond
                           (< bmi 18.5) ["orange" "underweight"]
                           (< bmi 25) ["inherit" "normal"]
                           (< bmi 30) ["orange" "overweight"]
                           :else ["red" "obese"])]
    [:div
     [:h3 "BMI calculator"]
     [:div
      "Height: " (int height) "cm"
      [slider :height height 100 220]]
     [:div
      "Weight: " (int weight) "kg"
      [slider :weight weight 30 150]]
     [:div
      "BMI: " (int bmi) " "
      [:span {:style {:color color}} diagnose]
      [slider :bmi bmi 10 50]]]))


(def assumptions (r/atom {:allowance-floor-price 16
                          :offsets-cap 8
                          :offset-benefiting-oregon 50
                          :offset-discount 20}))

(def exempts (r/atom (set '(:other-electricity))))

(def allowances (r/atom {:electric-utilities 1.0
                         :direct-electricity-service-suppliers 0}))

(def results (r/atom {}))

(defn calc-carbon []
  (let [{:keys [:allowance-floor-price :offsets-cap :offset-benefiting-oregon :offset-discount] :as assumptions} @assumptions
        {:keys [:sum] :as results} @results]
    (if (nil? sum)
      (assoc results :sum  (sum (vals assumptions))))))

(defn assumptions-slider [param value min max]
  [:input {:type "range" :value value :min min :max max
           :style {:width "100%"}
           :on-change (fn [e]
                        (swap! assumptions assoc param (.. e -target -value))
                        (when (not= param :sum)
                          (swap! results assoc :sum nil)))}])

(defn assumptions-table []
  (let [{:keys [:allowance-floor-price :offsets-cap :offset-benefiting-oregon :offset-discount] :as assumptions} @assumptions]
    [:fieldset
     [:legend "Assumptions Table"]
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
                       [0 "assumption" 1 "slider" 2 "value"])]))


(defn home-page []
  [:div
   [:h2 "Carbon Tax Calculator"]
   [assumptions-table]])
   ;;[bmi-component]])




;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
