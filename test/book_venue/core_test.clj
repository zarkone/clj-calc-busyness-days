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
