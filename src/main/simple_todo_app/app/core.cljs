(ns simple-todo-app.app.core
  (:require [reagent.dom :as rdom]))

(defn todo-app []
  [:h1 "Create Reagent todo-app"])

(defn render []
  (rdom/render [todo-app] (.getElementById js/document "root")))

(defn ^:export main []
  (render))

(defn ^:dev/after-load reload! []
  (render))
