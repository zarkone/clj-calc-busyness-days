(ns book-venue.core
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.set :as set]))


(defn- to-weekday [start-date date]
  (->> (t/days date)
       (t/plus start-date)
       (t/day-of-week)))


(defn cut-first-day [start-date days]
  (let [[weekday hours] (first days)
        hour (t/hour start-date)]
    (concat [[weekday
              (set/intersection
               (set (range hour 24))
               hours)]]
            (rest days))))


(defn cut-last-day [end-date days]
  (let [[weekday hours] (last days)
        hour (t/hour end-date)]
    (concat (butlast days)
            [[weekday
              (set/intersection
               (set (range 0 hour))
               hours)]])))


(defn- days-between [start-date end-date]
  (let [days (-> start-date
                 (t/interval end-date)
                 (t/in-days))]
    (->> (range 0 (inc days))
         (map (comp #(vector % (set (range 0 24)))
                    #(to-weekday start-date %)))
         (cut-first-day start-date)
         (cut-last-day end-date))))


(defn schedule-part-to-map [schedule-part]
  (->> (:schedule/weekdays schedule-part)
       (map #(vector % (:schedule/hours schedule-part)))
       (into {})))


(defn schedule-to-map [schedule]
  (apply merge-with (comp set concat)
         (map schedule-part-to-map
              schedule)))

(schedule-to-map [{:schedule/weekdays #{1 2 3}
                   :schedule/hours #{9 10 11 12 13 14 15 16}}
                  {:schedule/weekdays #{4 5}
                   :schedule/hours #{9 10 11 12 13 14}}])

(schedule-to-map [{:schedule/weekdays #{1 2 3}
                   :schedule/hours #{9 10}}

                  {:schedule/weekdays #{1 2 3}
                   :schedule/hours #{14 15 16}}])
;; {1 #{15 9 14 16 10}, 3 #{15 9 14 16 10}, 2 #{15 9 14 16 10}}

(defn make-calc-business-hours-for-day [schedule-map]
  (fn [[weekday hours]]
    (when-let [busyness-hours (get schedule-map weekday)]
       [weekday (set/intersection busyness-hours hours)])))

(let [schedule-map (schedule-to-map [{:schedule/weekdays #{1 2 3}
                                      :schedule/hours #{9 10}}

                                     {:schedule/weekdays #{1 2 3}
                                      :schedule/hours #{14 15 16}}])
      days (days-between
            (tc/from-date #inst "2018-03-04T07:00")
            (tc/from-date #inst "2018-03-06T12:00")
            )
      ]
  (->> days
       (map (make-calc-business-hours-for-day schedule-map))
       (remove nil?)
       (reduce #(+ %1 (-> %2 second count)) 0)))
(sort
 (cut-last-day (tc/from-date #inst "2018-03-02T12:00")
               (cut-first-day (tc/from-date #inst "2018-03-02T07:00") (set (range 0 24)))))

(t/hour (tc/from-date #inst "2018-03-02T07:00"))
(t/hours (tc/from-date #inst "2018-03-02T07:00"))

(days-between
 (tc/from-date #inst "2018-03-02T07:00")
 (tc/from-date #inst "2018-03-03T12:00")
 )
;; ([5 #{7 20 15 21 13 22 17 12 23 19 11 9 14 16 10 18 8}] [6 #{0 7 1 4 6 3 2 11 9 5 10 8}])

(defn calculate-business-hours [schedule start-date end-date]
  (let [schedule-map (schedule-to-map schedule)
        days (days-between (tc/from-date start-date)
                           (tc/from-date end-date))]
    (->> days
         (map (make-calc-business-hours-for-day schedule-map))
         (remove nil?)
         (reduce #(+ %1 (-> %2 second count)) 0)
         )))
