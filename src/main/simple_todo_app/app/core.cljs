(ns simple-todo-app.app.core
  (:require
   [reagent.core :as r]
   [reagent.dom :as d]
   [clojure.string :as str]
   [cljs.pprint :as pp]
   [cljs.reader :as reader]))

;;  --- App State ---

(defonce todos (r/atom (sorted-map)))

;; --- Local Storage ---

(def local-store-key "todo-app")

(defn todos->local-store []
  (.setItem js/localStorage local-store-key (str @todos)))

(defn local-store->todos []
  (let [edn-map-todos (.getItem js/localStorage local-store-key)
        unsorted-todos (some->> edn-map-todos reader/read-string)
        sorted-todos (into (sorted-map) unsorted-todos)]
    (reset! todos sorted-todos)))

;; --- Watch State ---

(add-watch todos :todos
           (fn [key _atom _old-state new-state]
             (todos->local-store)
             (println "---" key "atom changed ---")
             (pp/pprint new-state)))

;;  --- Utilities ---

(defn allocate-next-id [todos]
  ((fnil inc 0) (last (keys todos))))

(defn add-todo [text]
  (let [id (allocate-next-id @todos)
        new-todo {:id id :title text, :done false}]
    (swap! todos assoc id new-todo)))

(defn toggle-done [id]
  (swap! todos update-in [id :done] not))

(defn save-todo [id title]
  (swap! todos assoc-in [id :title] title))

;; (defn edit-todo [id title]
;;   (swap! todos assoc-in [id :title] title))

(defn delete-todo [id]
  (swap! todos dissoc id))

;; --- Initialize App with Sample Data ---

#_(defonce init (do
                  (add-todo "Task 1")
                  (add-todo "Task 2")
                  (add-todo "Task 3")))

;;  --- Views ---


(defn todo-input [{:keys [on-save]}]
  (let [input-text (r/atom "")
        update-text #(reset! input-text %)
        stop #(reset! input-text "")
        save #(let [trimmed-text (-> @input-text str str/trim)]
                (if-not (empty? trimmed-text) (on-save trimmed-text))
                (stop))
        key-pressed #(case %
                       "Enter" (save)
                       "Esc" (stop)
                       "Escape" (stop)
                       nil)]
    (fn [{:keys [class placeholder]}]
      [:div
       [:input {:class class
                :placeholder placeholder
                :auto-focus true
                :type "text"
                :value @input-text
                :on-change #(update-text (.. % -target -value))
                :on-key-down #(key-pressed (.. % -key))}]
       [:button {:class "add-todo"
                 :on-click save} "Add Task"]])))

(defn todo-item [_props-map]
  (let [editing (r/atom false)
        edited-title (r/atom "")]
    (fn [{:keys [id title done]}]
      [:section.todo-list
       [:div.todo-list-item {:class (when done "completed")}
        [:div.round
         [:input {:type "checkbox"
                  :id (str "checkbox-" id)
                  :checked done
                  :on-change #(toggle-done id)}]
         [:label {:for (str "checkbox-" id)
                  :class (when done "completed")}]]
        (if (not @editing)
          [:label.content {:class (when done "completed")
                           :on-double-click #(reset! editing true)} title]
          [:div.edit-todo
           [:input {:class "edit-field"
                    :type "text"
                    :value @edited-title
                    :on-change #(reset! edited-title (.. % -target -value))
                    :on-key-down #(when (= "Enter" (.. % -key))
                                    (do (save-todo id @edited-title)
                                        (reset! editing false)))}]])
       [:div.todos-btn
        ;; [:button.edit "Edit"]
        [:button.delete {:on-click #(delete-todo id)} "Delete"]]]])))

(defn task-list []
  (let [items (vals @todos)]
    [:section.todo-list
      (for [todo items]
        ^{:key (:id todo)} [todo-item todo])]))

(defn task-entry []
  [:section.create-todo
   [:h1 "MY TASK MANAGER 📝"]
   [todo-input {:class "new-todo"
                :placeholder "Type your todo here..."
                :on-save add-todo}]])

(defn todo-app []
   [:section
    [task-entry]
    (when (seq @todos)
      [:div
       [task-list]])])

;; --- Render ---

(defn render []
  (d/render [todo-app] (.getElementById js/document "root")))

(defn ^:export main []
  (render))

(defn ^:dev/after-load reload! []
  (render))
