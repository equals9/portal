(ns portal.ui.viewer.table
  (:require [portal.colors :as c]
            [portal.ui.inspector :as ins :refer [inspector]]
            [portal.ui.lazy :as l]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(defn- get-styles []
  (let [theme (theme/use-theme)]
    {:white-space :nowrap
     :border-bottom [1 :solid (::c/border theme)]
     :border-right [1 :solid (::c/border theme)]}))

(defn- table-header-row [child]
  [s/th
   {:style
    (assoc
     (get-styles)
     :top 0
     :z-index 2
     :position :sticky
     :background (ins/get-background))}
   child])

(defn- table-header-column [child]
  [s/th
   {:style
    (assoc
     (get-styles)
     :left 0
     :z-index 1
     :position :sticky
     :text-align :right
     :background (ins/get-background))}
   child])

(defn- table-data [_settings child]
  [s/td {:style (get-styles)} child])

(defn- table [settings component rows cols]
  (let [theme (theme/use-theme)
        transpose? false]
    [s/table
     {:style
      {:width "100%"
       :border-top [1 :solid (::c/border theme)]
       :border-left [1 :solid (::c/border theme)]
       :background (ins/get-background)
       :border-spacing 0
       :position :relative
       :color (::c/text theme)
       :font-size  (:font-size theme)
       :border-radius (:border-radius theme)}}
     [s/tbody
      [l/lazy-seq
       settings
       (map-indexed
        (fn [row-index row]
          [s/tr
           {:key row-index
            :style/hover
            {:background (str (::c/border theme) "55")}}
           (map-indexed
            (fn [col-index col]
              ^{:key col-index}
              [component
               (if-not transpose?
                 {:row row    :row-index row-index
                  :column col :column-index col-index}
                 {:row col    :row-index col-index
                  :column row :column-index row-index})])
            (if transpose? rows cols))])
        (if transpose? cols rows))]]]))

(defn- inspect-map-table [settings values]
  (let [columns (into #{} (mapcat keys (vals values)))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         (cond
           (= column row ::header) [table-header-row]

           (= row ::header)
           [table-header-row
            [inspector (assoc settings :coll values) column]]

           (= column ::header)
           [table-header-column
            [inspector (assoc settings :coll values) (first row)]]

           :else
           [table-data
            settings
            (let [[_ row] row]
              (when (contains? row column)
                [inspector (assoc settings :coll row :k column) (get row column)]))])))
     (concat [::header] (ins/try-sort-map values))
     (concat [::header] (ins/try-sort columns))]))

(defn- inspect-coll-table [settings values]
  (let [columns (into #{} (mapcat keys values))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         (cond
           (= column row ::header) [table-header-row]

           (= row ::header)
           [table-header-row
            [inspector (assoc settings :coll values) column]]

           (= column ::header)
           [table-header-column
            [inspector (assoc settings :coll values) (dec (:row-index context))]]

           :else
           [table-data
            settings
            (when (contains? row column)
              [inspector (assoc settings :coll row :k column) (get row column)])])))
     (concat [::header] values)
     (concat [::header] (ins/try-sort columns))]))

(defn- inspect-vector-table [settings values]
  (let [n (reduce max (map count values))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         [table-data
          settings
          (when (< column (count row))
            [inspector (assoc settings :coll row :k column) (get row column)])]))
     values
     (range n)]))

(defn- inspect-set-table [settings values]
  (let [columns (into #{} (mapcat keys values))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         (if (= row ::header)
           [table-header-row
            [inspector (assoc settings :coll row) column]]

           [table-data
            settings
            (when (contains? row column)
              [inspector (assoc settings :coll row :k column) (get row column)])])))
     (concat [::header] (ins/try-sort values))
     (ins/try-sort columns)]))

(defn- get-component [value]
  (cond
    (and (or (vector? value) (list? value))
         (every? vector? value))
    inspect-vector-table

    (and (map? value) (every? map? (vals value)))
    inspect-map-table

    (and (set? value) (every? map? value))
    inspect-set-table

    (and (coll? value) (every? map? value))
    inspect-coll-table))

(defn table-view? [value] (some? (get-component value)))

(defn inspect-table [settings values]
  (let [component (get-component values)]
    [ins/inc-depth [component settings values]]))

(def viewer
  {:predicate table-view?
   :component inspect-table
   :name :portal.viewer/table})
