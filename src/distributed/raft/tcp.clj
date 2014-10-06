(ns distributed.raft.tcp
  (:import (java.nio ByteBuffer))
  (:require
            [distributed.raft.protocol :as proto]
            [gloss.core :as gloss]
            [gloss.io :as gloss-io]
            [gloss.data.bytes.core :as gloss-bts]
            [taoensso.nippy :as nippy]
            [aleph.tcp :as tcp]
            [lamina.core :as lamina]
            [com.stuartsierra.component :as component]))

(defonce frame (gloss/compile-frame (gloss/finite-block :int32)))

(defn- decode-request
  "Change the gloss byte sequence -> ByteBuffer -> byte-array -> nippy/thaw"
  [buff-seq]
  (let [^ByteBuffer byte-buff (gloss-io/contiguous buff-seq)
        len (.limit byte-buff)
        ^"[B" bts (byte-array len)]
    (.get byte-buff bts)
    (nippy/thaw bts)))

(defn- encode-response [resp]
  (nippy/freeze resp))

(defn- create-handler [handler-f]
  (fn [ch client-info]
    (lamina/receive-all ch
                        #(lamina/enqueue ch (-> % decode-request
                                                  handler-f
                                                  encode-response)))))

(defn client!
  "Create a tcp client
   host => localhost
   port => integer port"
  [conf]
  (io!
    (lamina/wait-for-result
      (tcp/tcp-client (assoc conf :frame frame)))))

(defn send-receive!
  "Send a message to the server that will be processed by handler-f
   Wait for a response"
  ([client msg]
   (send-receive! client msg 10000))
  ([client msg timeout]
   (io!
     (lamina/enqueue client (nippy/freeze msg))
     (decode-request (lamina/wait-for-message client timeout)))))

(defn receive! [client timeout]
  (io!
    (decode-request (lamina/wait-for-message client timeout))))

(defn start-server!
  "Start a tcp server
   conf      => must have port defined
   handler-f => function called when a response is received the first argument is the nippy/thaw of the bytes sent"
  [conf handler-f]
  (io!
    (tcp/start-tcp-server (create-handler handler-f) (assoc conf :frame frame))))


(defn stop-server! [server]
  (io!
    (server)))

(defn stop-client! [client]
  (io!
    (lamina/close client)))

(defn- _ensure-client
  "Lockless client create cache, all client creates are in a delay wrapper
   Returns [state (delay client)]"
  [state k]
  (if-let [c (get state k)]
    c
    (let [c (delay (client! k))]
      (assoc state k c))))

(defn- ensure-client-in-ref!
  "Ensures that the client is in the client-state ref and returns a delay instance of creating the client"
  [client-state host port]
       (let [k {:host host :port port}]
         (if-let [c (get @client-state k)]
           c
           (get (dosync (alter client-state _ensure-client k)) k))))

(defn- _create-net-protocol-component!
  "Create/start : append-server, vote-server and a clients-state ref"
  [vote-port append-port write-port vote-handler append-handler write-handler]
  (let [vote-server   (start-server! {:port vote-port}   vote-handler)
        append-server (start-server! {:port append-port} append-handler)
        write-server  (start-server! {:port write-port}  write-handler)
        clients-state (ref {})]
    {:vote-port vote-port
     :append-port append-port
     :vote-server vote-server
     :append-server append-server
     :write-port write-port
     :write-server write-server
     :clients-state clients-state}))

(defn- _stop-net-protocol-component! [{:keys [write-server vote-server append-server clients-state]}]
  (stop-server! vote-server)
  (stop-server! append-server)
  (stop-server! write-server)

  (doseq [[k c] @clients-state]
    (stop-client! @c)
    (dosync (alter clients-state dissoc k))))

(defn- send-tcp-vote-req! [{:keys [clients-state vote-port] :as conf} member vote-req]
  (send-receive! @(ensure-client-in-ref! clients-state member vote-port)
                 vote-req))

;send-tcp-vote-req-async!

(defn nil-safe-get [m k default]
  (if-let [v (get m k)]
    v
    default))
;;A component that implements the Lifecycle and INet protocol
(defrecord TcpNetProtocolComponent [conf vote-handler append-handler write-handler]

  component/Lifecycle
  (start [this]
    (if (:proto-comp this)
      this
      (assoc this
        :proto-comp (_create-net-protocol-component! (nil-safe-get (:conf this) :vote-port   7090)
                                                     (nil-safe-get (:conf this) :append-port 7091)
                                                     (nil-safe-get (:conf this) :write-port  7092)
                                                     vote-handler
                                                     append-handler
                                                     write-handler))))
  (stop [this]
    (if (:proto-comp this)
      (do
        (_stop-net-protocol-component! (:proto-comp this))
        (dissoc this :proto-comp))))

  proto/INet
  (send-vote! [this member vote-req]
    (if (:proto-comp this)
      (send-tcp-vote-req! (:proto-comp this) member vote-req)
      (throw (RuntimeException. (str "The component was not started")))))
  (send-vote-async! [this member vote-req callback-f]
    (if (:proto-comp this)
      (send-tcp-vote-req-async! (:proto-comp this) member vote-req callback-f)
      (throw (RuntimeException. (str "The component was not started"))))))


(defn create-net-protocol-component [conf vote-handler append-handler write-handler]
  (->TcpNetProtocolComponent conf vote-handler append-handler write-handler))