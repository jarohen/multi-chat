(ns multi-chat.handler
  (:require [ring.util.response :refer [response]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [resources]]
            [compojure.handler :refer [api]]
            [hiccup.page :refer [html5 include-css include-js]]
            [frodo :refer [repl-connect-js]]
            [chord.http-kit :refer [with-channel]]
            [clojure.tools.reader.edn :as edn]
            [clojure.core.async :as a :refer [go go-loop]]))

(defn page-frame []
  (html5
   [:head
    [:title "Multiplayer Chat"]
    (include-js "//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js")
    (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
    (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")

    (include-js "/js/multi-chat.js")]
   [:body
    [:div#content]
    [:script (repl-connect-js)]]))

(defonce !last-id (atom 0))

(defn new-user-id! [{:keys [remote-addr] :as req}]
  (str remote-addr "-" (swap! !last-id inc)))

(defonce !open-chs (agent #{}))

(defonce !recent-messages (atom []))

(defn push-messages! [messages ch]
  (a/put! ch (pr-str (reverse messages))))

(defn broadcast-messages! [chs messages]
  (doseq [ch chs]
    (push-messages! messages ch)))

(defn process-message! [id {:keys [chat]}]
  (swap! !recent-messages
         (fn [messages]
           (let [new-messages (take 10 (cons {:id id :chat chat} messages))]
             (send-off !open-chs #(doto % (broadcast-messages! new-messages)))
             new-messages))))

(defn add-ch [conns ch]
  (push-messages! @!recent-messages ch)
  (conj conns ch))

(defn listen-for-messages! [user-id ch]
  (go-loop []
    (if-let [{:keys [message]} (a/<! ch)]
      (do
        (process-message! user-id (edn/read-string message))
        (recur))
      (send !open-chs disj ch))))

(defn user-joined! [req]
  (with-channel req ch
    (send-off !open-chs add-ch ch)
    (listen-for-messages! (new-user-id! req) ch)))

(defroutes app-routes
  (GET "/" [] (response (page-frame)))
  (GET "/messages" [] user-joined!)
  (resources "/js" {:root "js"}))

(def app 
  (-> app-routes
      api))
