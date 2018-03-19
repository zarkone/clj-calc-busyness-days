(ns book-venue.core-test
  (:require [clojure.test :refer :all]
            [book-venue.core :refer :all]))

(deftest eight-test
  (testing "8 hours"
    (let [schedule [{:schedule/weekdays #{1 2 3 4 5}
                     :schedule/hours #{9 10 11 12 13 14 15 16}}]]
      (is
       (= 8
          (calculate-business-hours schedule #inst "2018-03-02T07:00" #inst "2018-03-04T07:00"))))))

(deftest eleven-test
  (testing "11 hours"
    (let [schedule [{:schedule/weekdays #{1 2 3 4 5}
                     :schedule/hours #{9 10 11 12 13 14 15 16}}]]
      (is
       (= 11
          (calculate-business-hours schedule #inst "2018-03-02T07:00" #inst "2018-03-05T12:00"))))))

;; ----------------------

(deftest separate-weekdays
  (testing "Separate weekdays in schedule"
    (let [schedule [{:schedule/weekdays #{1 2 3}
                     :schedule/hours #{9 10 11 12 13 14 15 16}}

                    {:schedule/weekdays #{4 5}
                     :schedule/hours #{9 10 11 12 13 14}}]]
      (is
       (= 20
          (calculate-business-hours schedule #inst "2018-02-28T07:00" #inst "2018-03-03T19:00"))))))

(deftest separate-hours
  (testing "Separate hours in schedule"
    (let [schedule [{:schedule/weekdays #{1 2 3}
                     :schedule/hours #{9 10}}

                    {:schedule/weekdays #{1 2 3}
                     :schedule/hours #{14 15 16}}]]
      (is
       (= 7
          (calculate-business-hours schedule #inst "2018-03-05T10:00" #inst "2018-03-06T15:00"))))))

(deftest one-day
  (testing "Separate hours in schedule"
    (let [schedule [{:schedule/weekdays #{1 2 3}
                     :schedule/hours #{9 10}}

                    {:schedule/weekdays #{1 2 3}
                     :schedule/hours #{14 15 16}}]]
      (is
       (= 2
          (calculate-business-hours schedule #inst "2018-03-05T10:00" #inst "2018-03-05T15:00"))))))

;;;; -----------------

(deftest eventum1
  (testing "Eventum test1"
    (let [schedule [{:schedule/weekdays #{6} :schedule/hours #{7}}]]
      (is
       (= 1
          (calculate-business-hours schedule #inst"2000-01-01T01:00" #inst"2000-01-02T00:00"))))))
