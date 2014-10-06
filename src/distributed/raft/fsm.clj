(ns distributed.raft.fsm
  (:require [clojure.core.async :as async]))

(comment
  ;here mem is memory or statefulness of the function, and state the transitional state
  trans-f [m x] -> [mem state output]
  timtout-f [m] -> [mem state]

  {:start [:follower {}]
   :end   :exit

   :transitions
          {:candidate {:f [req-ch candidate-trans-f resp-ch] :timeout [candidate-time-out-f 1000]}
           :follower  {:f [req-ch follower-trans-f  resp-ch] :timeout [follower-time-out-f  1000]}
           :leader    {:f [req-ch leader-trans-f resp-ch] :timeout    [leader-time-out-f    1000]}
           }})

(defn async-run-fsm!
  "A FSM for moving data between async channels with timeouts"
  [m]
  (let [transitions (:transitions m)
        end-state   (:end m)]
    (async/go-loop
      [[m2 state] (:start m)]
      (let [{:keys [f timeout]} (get transitions state)
            [req-ch trans-f resp-ch] f
            [timeout-f timeout-ms] timeout
            [v ch] (async/alts! [req-ch (async/timeout timeout-ms)])]
        (if v
          (let [res (trans-f m2 v)]
            (when-not (= (nth res 1) end-state)
              (async/>! resp-ch (nth res 2))
              (recur res)))
          (recur (timeout-f m2)))))))



(defn test-state []
  (let [test-in-ch (async/chan 10)
        test-out-ch (async/chan 10)
        timeout-f (fn [m] [m :exit nil])
        trans-a-b (fn [m v]
                    (prn "Trans :a [" v "] => b")
                    (Thread/sleep 1000)
                    [m (if (> v 4) :exit :b) [:a v]])
        trans-b-a (fn [m v]
                    (prn "Trans :b [" v "] => a")
                    (Thread/sleep 1000)
                    [m :a [:b v]])]

    (async/go-loop []
      (when-let [v (async/<! test-out-ch)]
        (prn "see output v: " v)
        (recur)))

    (async/go-loop [i 1]
                   (async/>! test-in-ch i)
                   (recur (inc i)))

    {:start [{} :a]
     :end :exit
     :transitions {:a {:f [test-in-ch trans-a-b test-out-ch] :timeout [timeout-f 10000]}
                   :b {:f [test-in-ch trans-b-a test-out-ch] :timeout [timeout-f 10000]}}
     }))