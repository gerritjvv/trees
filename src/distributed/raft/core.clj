(ns distributed.raft.core
    (:require [clojure.core.async :as async]
              [distributed.raft.protocol :as proto]
              [distributed.raft.tcp :as tcp]
              [com.stuartsierra.component :as component]))


(defrecord LogEntry [cmd ^long term ^long index])
(defrecord VoteRequest [^long term id ^LogEntry log-entry])
(defrecord VoteResponse [^boolean granted? ^long term])

(defrecord AppendRequest [])

(defn up-to-date?
  "Takes two LogEntry(s) if entry1 has a term bigger than entry2 or entry1's index is >= than entry2's returns true"
  [entry1 entry2]
  (cond
    (> (:term entry1) (:term entry2)) true
    (and
      (= (:term entry1) (:term entry2))
      (>= (:index entry1) (:index entry2))) true
    :else false))

(defn request-vote!
  "A candidate sends a vote
   state => must be created with the create-state function"
  ([state member term id log-entry]
   (request-vote! state member (->VoteRequest term id log-entry)))
  ([state member vote-req]
   (proto/send-vote! (:net-protocol state) member vote-req)))


(defn vote
  "Any server handles a vote request and sent by send-vote"
  [vote-state ^Long current-term receiver-log-entry {:keys [^Long term id log-entry] :as vote-req}]
  (let [voted? (get vote-state current-term)
        granted? (cond
                   (< term current-term) false
                   voted? false
                   (and (not voted?)
                        (up-to-date?  log-entry receiver-log-entry)) true
                   :else false)]
    (->VoteResponse granted? current-term)))

(defn random-timeout []
  (+ (rand-int (- 300 150)) 150))

(defn handle-append [state v ]
  (prn "Appending state " v))

(defmaco handle-follower [state append-req-ch append-resp-ch timeout]
  `(let [[v# _] (async/alt! [~append-req-ch ~timeout])]
     (if v
       [(handle-append ~state v#) :follower]
       [~state :candidate])))

(defn create-kernel-loop [vote-req-ch vote-resp-ch append-req-ch append-resp-ch ]
  (async/go
    (while true
      ;@TODO HANDLE STATES and vote requests responses.
      (loop [state nil server-state :follower timeout (async/timeout (random-timeout))]
       (let [[state2 server-state2]
              (condp = state
                :follower (handle-follower state append-req-ch append-resp-ch timeout))])))))

(defn _create-state
  "vote-resp-channel and append-resp-channels are clojure.core.async channels
   to which any vote or append responses will be sent

   The default netty tcp protocol will be used, a custom protocol can be injected
   by passing in a :net-protocol-f function that takes as its arguments the vote-resp-channel and append-resp-channel,
   the function must return an object that implements the INet protocol"
  [vote-req-ch vote-resp-ch append-req-ch append-resp-ch {:keys [vote-port append-port net-protocol-f]}]
  (let [vote-handler   #(do
                         (async/>!! vote-req-ch %)
                         (async/<!! vote-resp-ch))
        append-handler #(do
                         (async/>!! append-req-ch %)
                         (async/<!! append-resp-ch))]
    (if net-protocol-f
      {:net-protocol (net-protocol-f vote-req-ch vote-resp-ch append-req-ch append-resp-ch )
       :kernel-loop (create-kernel-loop vote-req-ch vote-resp-ch append-req-ch append-resp-ch )}
      {
        :kernel-loop (create-kernel-loop vote-req-ch vote-resp-ch append-req-ch append-resp-ch )
        :net-protocol (component/start
                       (tcp/create-net-protocol-component {:vote-port vote-port :append-port append-port}
                                                          vote-handler
                                                          append-handler))})))


(defrecord RaftKernel [conf]
  component/Lifecycle
  (start [this]
    (if (:raft-kernel this)
      this
      (let [vote-resp-ch (async/chan)
            vote-req-ch (async/chan)
            append-resp-ch (async/chan)
            append-req-ch (async/chan)]
        (assoc this :vote-req-ch vote-req-ch
                    :append-req-ch append-req-ch
                    :vote-resp-ch vote-resp-ch
                    :append-resp-ch append-resp-ch
                    :raft-kernel
                    (_create-state vote-req-ch vote-resp-ch append-req-ch append-resp-ch (:conf this))))))

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
