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
         (reduce #(+ %1 (-> %2 second count)) 0))))


;; Task 3

(defn- maybe-add-week [weekday-to-forward date-weekday]
  (if (< weekday-to-forward date-weekday)
    (+ 7 weekday-to-forward)
    weekday-to-forward))

(defn- forward-till-weekday [weekday-to-forward date]
  (let [date-weekday (t/day-of-week date)]
    (->> date-weekday
         (- (maybe-add-week weekday-to-forward date-weekday))
         (t/days)
         (t/plus date)
         (t/with-time-at-start-of-day))))


(defn busyness-days-seq
  ([schedule-map] (busyness-days-seq schedule-map 0))
  ([schedule-map n]
   (let [weekdays (-> schedule-map keys sort)
         next-weekday (nth weekdays (mod n (count weekdays)))
         weekday-schedule (-> schedule-map (get next-weekday) sort)]
     (lazy-seq
      (cons [next-weekday weekday-schedule]
            (busyness-days-seq schedule-map (inc n)))))))


(defn- busyness-days-from-date [start-date schedule-map]
  (->> (busyness-days-seq schedule-map)
       (drop-while #(< (first %)
                       (t/day-of-week start-date)))))

(defn normalize-start-date [from schedule-map]
  (let [start-date (->> from tc/from-date)
        schedule-weekdays (-> schedule-map keys sort)
        start-weekday (t/day-of-week start-date)
        next-weekday (or (->> schedule-weekdays
                              (drop-while #(< % start-weekday))
                              first)
                         (first schedule-weekdays))]
    (if-not (get schedule-map start-weekday)
      (forward-till-weekday next-weekday start-date)
      start-date)))


(defn- calc-overflow-day [next-schedule next-weekday finish-date]
  (let [next-busyness-day (forward-till-weekday next-weekday finish-date)]
    (->> next-schedule
         (first)
         (t/hours)
         (t/plus next-busyness-day))))


(defn- calc-final-day [next-schedule busyness-hours finish-date]
  (let [next-hour (nth next-schedule busyness-hours)]
    (-> finish-date
        (t/with-time-at-start-of-day)
        (t/plus (t/hours next-hour)))))


(defn- forward-busyness-hours
  [{:keys [busyness-hours finish-date] :as state}
   [next-weekday next-schedule]]
  (if (>= 0 busyness-hours)
    (->> finish-date
         (calc-overflow-day next-schedule next-weekday)
         (reduced))
    (let [next-hours (count next-schedule)]
      (if (> next-hours busyness-hours)
        (->> finish-date
             (calc-final-day next-schedule busyness-hours)
             (reduced))
        (-> state
            (update :finish-date #(forward-till-weekday next-weekday %))
            (update :busyness-hours #(- % (count next-schedule))))))))


(defn calculate-date [schedule from business-hours]
  (let [schedule-map (schedule-to-map schedule)
        start-date (normalize-start-date from schedule-map)
        busyness-days (busyness-days-from-date start-date schedule-map)]
    (->> busyness-days
         (reduce forward-busyness-hours
                 {:finish-date start-date
                  :busyness-hours business-hours})
         (tc/to-date))))
