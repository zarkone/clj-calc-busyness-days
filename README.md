## Task 2: Date and time calculations

### Description

Each venue has different opening hours for their booking office.

Basically a time window for when they are available to handle incoming booking requests and customer enquiries.

Most of them have a simple time window like monday-friday 09:00 to 17:00, but some may have more complex schedules like monday-wednesday 09-17 and thursday-friday 09-15 and some may even have schedules like monday-wednesday 09-11, 14-17 and thursday-friday: closed.

Each schedule is represented as a map with the following specification

Key | Type | Description
----| ---- | -----------
:schedule/weekdays| set of longs (1-7) | Weekdays schedule is valid (1=monday, 7=sunday)
:schedule/hours | set of longs (0-24) | Hours schedule is valid

Venues may have multiple schedules.

#### Examples

##### Monday - friday 09-17

```clojure
[{:schedule/weekdays #{1 2 3 4 5}
  :schedule/hours #{9 10 11 12 13 14 15 16}}]
```

##### Monday - wednesday 09-17, thursday - friday 09-15

```clojure
[{:schedule/weekdays #{1 2 3}
  :schedule/hours #{9 10 11 12 13 14 15 16}}

 {:schedule/weekdays #{4 5}
  :schedule/hours #{9 10 11 12 13 14}}]
```

##### Monday - wednesday 09-11, 14-17 and thursday - friday: closed

```clojure
[{:schedule/weekdays #{1 2 3}
  :schedule/hours #{9 10}}

 {:schedule/weekdays #{1 2 3}
  :schedule/hours #{14 15 16}}]
```

### Objective

#### Implement a function that calculates the number of business hours that has elapsed between two dates


Function signature:

```clojure
(defn calculate-business-hours
  [schedule start-date end-date])
```

Examples

```clojure
(let [schedule [{:schedule/weekdays #{1 2 3 4 5}
                 :schedule/hours #{9 10 11 12 13 14 15 16}}]]
  (calculate-business-hours schedule #inst "2018-03-02T07:00" #inst "2018-03-04T07:00"))
;; => 8

(let [schedule [{:schedule/weekdays #{1 2 3 4 5}
                 :schedule/hours #{9 10 11 12 13 14 15 16}}]]
  (calculate-business-hours schedule #inst "2018-03-02T07:00" #inst "2018-03-05T12:00"))
;; => 11
```

Please ignore any timezone related issues. Just assume everything is UTC
