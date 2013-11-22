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

(defn new-user-id! []
  (str "user-" (swap! !last-id inc)))

(defonce !connections (atom #{}))

(defonce !recent-messages (agent []))

(defn send-messages-to-client! [messages ch]
  (->> messages
       reverse
       pr-str
       (a/put! ch)))

(defn process-message! [messages id {:keys [chat]}]
  (let [new-messages (take 10 (cons {:id id :chat chat} messages))]
    (doseq [ch @!connections]
      (send-messages-to-client! new-messages ch))
    new-messages))

(defn listen-for-messages! [ch]
  (let [user-id (new-user-id!)]
    (go-loop []
      (if-let [{:keys [message]} (a/<! ch)]
        (do
          (send-off !recent-messages process-message! user-id (edn/read-string message))
          (recur))
        (swap! !connections disj ch)))))

(defn user-joined! [req]
  (with-channel req ch
    (swap! !connections conj ch)
    (send-messages-to-client! @!recent-messages ch)
    (listen-for-messages! ch)))

(defroutes app-routes
  (GET "/" [] (response (page-frame)))
  (GET "/messages" [] user-joined!)
  (resources "/js" {:root "js"}))

(def app 
  (-> app-routes
      api))
