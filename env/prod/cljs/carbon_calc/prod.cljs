(ns carbon-calc.prod
  (:require
    [carbon-calc.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
