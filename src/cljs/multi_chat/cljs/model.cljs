(ns multi-chat.cljs.model
  (:require [cljs.core.async :as a]
            [chord.client :refer [ws-ch]]
            [cljs.reader :refer [read-string]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn open-ws [route]
  (ws-ch (str "ws://" js/location.host route)))

(defn watch-recd-messages! [server-ch !messages]
  (go-loop []
    (reset! !messages (read-string (:message (a/<! server-ch))))
    (recur)))

(defn watch-sent-messages! [server-ch sent-messages-ch]
  (go-loop []
    (a/>! server-ch (pr-str (a/<! sent-messages-ch)))
    (recur)))

(defn wire-up-messages! [!messages sent-messages-ch]
  (go
   (let [server-ch (a/<! (open-ws "/messages"))]
     (watch-sent-messages! server-ch sent-messages-ch)
     (watch-recd-messages! server-ch !messages))))
