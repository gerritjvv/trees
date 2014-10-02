(ns distributed.raft.protocol)


(defprotocol INet
  (send-vote! [this member vote-request] "Send a vote request over the network protocol")
  ;@TODO add append request
  )