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

;; Task 3

(defn- drop-till-next-suitable-weekday [weekday sorted-suitable-weekdays]
  (let [dropped (drop-while #(> weekday %) sorted-suitable-weekdays)]
    (set
     (if (empty? dropped)
       sorted-suitable-weekdays
       dropped))))

(defn- calc-next-suitable-weekday [date schedule-map]
  (let [weekday (t/day-of-week date)]
    (->> schedule-map
         keys
         sort
         (drop-till-next-suitable-weekday weekday)
         first
         )))


;; (t/day-of-week (t/plus (t/today) (t/days 4)))
#_(calc-suitable-weekdays (t/plus (t/today) (t/days 5))
                            (schedule-to-map
                             [{:schedule/weekdays #{1 5}
                               :schedule/hours #{9 10 11 12 13 14 15 16}}
                              {:schedule/weekdays #{4}
                               :schedule/hours #{17}}]))

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
         (t/with-time-at-start-of-day)
         )))

;; (defn- take-next-suitable-date [date schedule-map]
;;   (let [weekday (t/day-of-week date)
;;         next-suitable-weekday ()])
;;   ())



(defn- maybe-suitable-date [date schedule-map]
  (let [weekday (t/day-of-week date)
        hour (t/hour date)
        suitable-hours (get schedule-map weekday)]
    (when (not (empty? suitable-hours))
      (when-let [suitable-hour (->> (sort suitable-hours)
                                    (drop-while #(> hour %))
                                    first)]
        (->> (- (inc suitable-hour) hour)
             (t/hours)
             (t/plus date))))))



(defn calculate-date [schedule from business-hours]
  (let [start-date (tc/from-date from)
        schedule-map (schedule-to-map schedule)
        with-added-hours (->> business-hours
                              t/hours
                              (t/plus start-date))]

    (if-let [with-added-hours-suitable (maybe-suitable-date with-added-hours schedule-map)]
      (tc/to-date with-added-hours-suitable)
      (let [next-day (-> with-added-hours
                         (t/plus (t/days 1))
                         (t/with-time-at-start-of-day))
            suitable-weekday (calc-next-suitable-weekday next-day schedule-map)]
        ;; (println suitable-weekday)
        (-> suitable-weekday
            (forward-till-weekday next-day)
            ;; (maybe-suitable-date schedule-map)
            (tc/to-date)
            )))
    ))


(schedule-to-map
 [{:schedule/weekdays #{5}
   :schedule/hours #{9 10 11 12 13 14 15 16}}
  {:schedule/weekdays #{4}
   :schedule/hours #{17}}])

(let [schedule [{:schedule/weekdays #{1 2 3 4 5}
                     :schedule/hours #{16}}]]
  (calculate-date schedule #inst"2018-03-16T01:00:00" 8))



;; 1. date fits schedule day and hours
;; 2. date fits schedule day but not hours, before
;; 3. date fits schedule day but not hours, after
;; 4. date doesn't fit schedule day, need to seek till next suitable weekday
;;    and count days
