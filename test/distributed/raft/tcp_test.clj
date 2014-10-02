(ns distributed.raft.tcp-test
  (:require [distributed.raft.tcp :refer :all]
            [distributed.raft.protocol :refer [send-vote!]]
            [com.stuartsierra.component :as component])
  (:use midje.sweet))

(defn- echo [x] x)

(fact "Test send an receive"
  (let [server (start-server! {:port 6000} echo)
        client (client! {:host "localhost" :port 6000})]

    (send-receive! client {:a 1 :b 2}) => {:a 1 :b 2}
    (stop-server! server)
    (stop-client! client)))


(fact "Test TcpNetProtocolComponent vote"
      (let [append-handler echo
            vote-handler echo
            comp (component/start
                   (create-net-protocol-component {} vote-handler append-handler))]
        ;[^long term id ^LogEntry log-entry]
        (send-vote! comp "localhost" {:term 1 :id 1 :log-entry {}}) => {:term 1 :id 1 :log-entry {}}
        (component/stop comp)
        ))