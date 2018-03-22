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


(defn busyness-days-seq
  ([schedule-map] (busyness-days-seq schedule-map 0))
  ([schedule-map n]
   (let [weekdays (-> schedule-map keys sort)
         next-weekday (nth weekdays (mod n (count weekdays)))
         weekday-schedule (get schedule-map next-weekday)]
     (lazy-seq
      (cons [next-weekday weekday-schedule]
            (busyness-days-seq schedule-map (inc n)))))))


(defn calculate-date [schedule from business-hours]
  (let [start-date (tc/from-date from)
        schedule-map (schedule-to-map schedule)

        busyness-days (->> (busyness-days-seq schedule-map)
                           (drop-while #(not= (first %)
                                              (t/day-of-week start-date))))]
    (->> busyness-days
         (reduce (fn [{:keys [busyness-hours finish-date] :as state}
                     [next-weekday next-schedule]]
                   (println [next-weekday next-schedule])
                   (if (= 0 busyness-hours)
                     (->> (sort next-schedule)
                          first
                          (t/hours)
                          (t/plus (t/with-time-at-start-of-day
                                    (forward-till-weekday next-weekday finish-date)
                                    ))
                          (reduced)
                          )
                     (let [next-hours (count next-schedule)]
                       (if (> next-hours busyness-hours)
                         (let [next-hour (-> next-schedule sort (nth busyness-hours))]
                           (-> (t/with-time-at-start-of-day finish-date)
                               (t/plus (t/hours next-hour))
                               (reduced)
                               ))
                         (-> state
                             (update :finish-date #(forward-till-weekday next-weekday %))
                             (update :busyness-hours #(- % (count next-schedule)))
                             ))
                       )))
                 {:finish-date start-date
                  :busyness-hours business-hours}
                 )
         (tc/to-date)
         )


    ))

(let [schedule [{:schedule/weekdays #{5}
                     :schedule/hours #{9 10 11 12 13 14 15 16}}
                    {:schedule/weekdays #{4}
                     :schedule/hours #{17}}]]
      (calculate-date schedule #inst"2018-03-16T01:00:00" 8))

#_(let [schedule [{:schedule/weekdays #{1 2 3 4 5}
                 :schedule/hours #{15 14 13 16}}]]

  (calculate-date schedule #inst"2018-03-16T01:00:00" 6))


#_(let [schedule [{:schedule/weekdays #{1 2}
                 :schedule/hours #{9 10 }}
                {:schedule/weekdays #{5}
                 :schedule/hours #{9 10 11 12 13 14 15 16}}
                {:schedule/weekdays #{4}
                 :schedule/hours #{17}}]]
  (calculate-date schedule #inst"2018-03-16T01:00:00" 8))


;; 1. date fits schedule day and hours
;; 2. date fits schedule day but not hours, before
;; 3. date fits schedule day but not hours, after
;; 4. date doesn't fit schedule day, need to seek till next suitable weekday
;;    and count days
