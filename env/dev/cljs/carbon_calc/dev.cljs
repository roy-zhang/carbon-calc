(ns ^:figwheel-no-load carbon-calc.dev
  (:require
    [carbon-calc.core :as core]
    [devtools.core :as devtools]))


(enable-console-print!)

(devtools/install!)

(core/init!)
