(ns carbon-calc.core
    (:require
      [reagent.core :as r]))

;; -------------------------
;; Views

(defn simple-component []
  [:div
   [:p "I'm a component!"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red "] "text."
    ]])

(defn home-page []
  [:div
   [:h2 "Welcome to Reagent"]
   [simple-component]]
  )

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
