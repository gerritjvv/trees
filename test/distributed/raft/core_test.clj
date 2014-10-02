(ns distributed.raft.core-test
  (:require [distributed.raft.core :refer :all])
  (:use midje.sweet))

(facts "Test distributed unit functions"
       (fact "Test up-to-date?"

             (up-to-date? {:term 1 :index 1} {:term 2 :index 1}) => false
             (up-to-date? {:term 2 :index 1} {:term 1 :index 1}) => true
             (up-to-date? {:term 1 :index 1} {:term 1 :index 1}) => true
             (up-to-date? {:term 1 :index 10} {:term 1 :index 1}) => true)

       (fact "Test vote response"
             ;[vote-state current-term receiver-log-entry {:keys [term id log-entry] :as vote-req}]
             (let [current-term 1
                   vote-state {1 1}]
               (vote vote-state current-term {} {:term 1}) => {:granted? false :term current-term})
             (let [current-term 1
                   vote-state {}]
               (vote vote-state current-term {:term 1} {:term 2 :log-entry {:term 10}}) => {:granted? true :term current-term})
             (let [current-term 10
                   vote-state {}]
               (vote vote-state current-term {:term 3} {:term 2  :log-entry {:term 10}}) => {:granted? false :term current-term})))