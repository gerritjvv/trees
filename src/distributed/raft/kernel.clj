(ns distributed.raft.kernel
  (:require [distributed.raft.fsm :as fsm]
            [distributed.raft.api :as api]
            [distributed.raft.tcp :as tcp]
            [clojure.core.async   :as async]
            [clojure.tools.logging :refer [info error]]
            [com.stuartsierra.component :as component]))

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


(defn follower-trans [state append-req]
  (info "follower")
  (Thread/sleep 1000)
  [state :follower])

(defn follower-timeout [state]
  (info "follower timeout")
  (Thread/sleep 1000)
  [state :candidate])

;for voting
(defn candidate-trans [net-protocol state append-req]
  (info "candidate")
  (Thread/sleep 1000)
  [state :candidate])

;for voting
(defn candidate-timeout [state]
  (info "candidate timeout")
  (Thread/sleep 1000)
  [state :follower])

;for leader
(defn write-trans [state append-req]
  (info "leader")
  (Thread/sleep 1000)
  [state :leader])

;for leader
(defn write-timeout [state]
  (info "leader-timeout")
  (Thread/sleep 1000)
  [state :leader])


(defn random-timeout []
  (+ (rand-int (- 300 150)) 150))


(defn create-kernel-loop [net-protocol
                          state vote-req-ch vote-resp-ch append-req-ch append-resp-ch write-req-ch write-resp-ch]
  (fsm/async-run-fsm! {:start [state :follower]
                       :end :exit
                       :transitions
                              {:follower  {:f [append-req-ch follower-trans  append-resp-ch] :timeout [follower-timeout  (random-timeout)]}
                               :candidate {:f [vote-req-ch   (partial candidate-trans net-protocol) vote-resp-ch  ] :timeout [candidate-timeout (random-timeout)]}
                               :leader    {:f [write-req-ch  write-trans     write-resp-ch ] :timeout [write-timeout     120000]}

                                }}))

(defn _create-state
  "vote-resp-channel and append-resp-channels are clojure.core.async channels
   to which any vote or append responses will be sent

   The default netty tcp protocol will be used, a custom protocol can be injected
   by passing in a :net-protocol-f function that takes as its arguments the vote-resp-channel and append-resp-channel,
   the function must return an object that implements the INet protocol"
  [vote-req-ch vote-resp-ch append-req-ch append-resp-ch write-req-ch write-resp-ch {:keys [vote-port append-port write-port net-protocol-f] :as conf}]
  (let [
         ;@TODO use a members component/service to get members
        state conf
        vote-handler   #(do
                         (async/>!! vote-req-ch %)
                         (async/<!! vote-resp-ch))
        append-handler #(do
                         (async/>!! append-req-ch %)
                         (async/<!! append-resp-ch))
        write-handler  #(do
                         (async/>!! write-req-ch %)
                         (async/<!! write-resp-ch))
        net-protocol (if net-protocol-f
                       (net-protocol-f vote-req-ch vote-resp-ch append-req-ch append-resp-ch )
                       (component/start
                         (tcp/create-net-protocol-component {:vote-port vote-port :append-port append-port :write-port write-port}
                                                            vote-handler
                                                            append-handler
                                                            write-handler)))]
    ;TODO pass in netprotocol
    {
      :kernel-loop (create-kernel-loop net-protocol
                                       state vote-req-ch vote-resp-ch append-req-ch append-resp-ch write-req-ch write-resp-ch)
      :net-protocol net-protocol}))


(defrecord RaftKernel [conf]
  component/Lifecycle
  (start [this]
    (if (:raft-kernel this)
      this
      (let [vote-resp-ch (async/chan)
            vote-req-ch (async/chan)
            append-resp-ch (async/chan)
            append-req-ch (async/chan)
            write-req-ch  (async/chan)
            write-resp-ch  (async/chan)]
        (assoc this :vote-req-ch vote-req-ch
                    :append-req-ch append-req-ch
                    :vote-resp-ch vote-resp-ch
                    :append-resp-ch append-resp-ch
                    :write-req-ch write-req-ch
                    :write-resp-ch write-resp-ch
                    :raft-kernel
                    (_create-state vote-req-ch vote-resp-ch append-req-ch append-resp-ch write-req-ch write-resp-ch (:conf this))))))

  (stop [this]
    (if (:raft-kernel this)
      (do
        (component/stop (get-in this [:raft-kernel :net-protocol]))
        (dissoc this :raft-kernel :vote-resp-ch :append-resp-ch))
      this)))

(defn create-raft-kernel
  "conf
     keys => :append-port :vote-port :net-protocol-f"
  [conf]
  (->RaftKernel conf))

