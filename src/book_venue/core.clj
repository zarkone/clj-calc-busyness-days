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

(defn at-midnight? [date]
  (= 0
     (t/hour date)
     (t/minute date)
     (t/second date)))

(defn- maybe-inc-days [days date]
  (if (at-midnight? date)
    (inc days)
    days))

(defn- days-between [start-date end-date]
  (let [days (-> start-date
                 (t/interval end-date)
                 (t/in-days)
                 (maybe-inc-days end-date))]
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


(defn make-calc-business-hours-for-day [schedule-map]
  (fn [[weekday hours]]
    (when-let [busyness-hours (get schedule-map weekday)]
       [weekday (set/intersection busyness-hours hours)])))


(defn calculate-business-hours [schedule start-date end-date]
  (let [schedule-map (schedule-to-map schedule)
        days (days-between (tc/from-date start-date)
                           (tc/from-date end-date))]
    (->> days
         (map (make-calc-business-hours-for-day schedule-map))
         (remove nil?)
         (reduce #(+ %1 (-> %2 second count)) 0)
         )))
