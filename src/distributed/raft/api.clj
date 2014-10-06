(ns distributed.raft.api
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

