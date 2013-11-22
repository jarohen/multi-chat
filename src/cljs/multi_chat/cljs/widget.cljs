(ns multi-chat.cljs.widget
  (:require [clojure.string :as s]
            [dommy.core :as d]
            [cljs.core.async :as a]
            [goog.events.KeyCodes :as kc])
  (:require-macros [dommy.macros :refer [node sel1]]
                   [cljs.core.async.macros :refer [go]]))

(defn messages-list-node [messages]
  (node
   [:ul
    (if (seq messages)
      (for [{:keys [id chat]} messages]
        [:li [:strong id] ": " [:em chat]])
      [:li [:strong "No messages yet."]])]))

(defn messages-box-node [bind-messages!]
  (doto (node [:div {:style {:border "1px solid black"
                             :border-radius "1em"
                             :padding "1em"
                             :margin-bottom "1em"}}])
    (bind-messages!)))

(defn textbox-node [bind-textbox!]
  (doto (node [:input.form-control {:type "text"
                                    :style {:width "100%"}}])
    (bind-textbox!)))

(defn focus! [$el]
  (go (a/<! (a/timeout 200))
      (.focus $el)))

(defn textbox-binder [sent-messages-ch]
  (fn [$textbox]
    (focus! $textbox)
    (d/listen! $textbox :keyup
               (fn [e]
                 (let [message (d/value $textbox)]
                   (when (and (= kc/ENTER (.-keyCode e))
                              (not (s/blank? message)))
                     (js/console.log "Sending:" message)
                     (a/put! sent-messages-ch {:chat message})
                     (d/set-value! $textbox nil)))))))

(defn messages-binder [!messages]
  (fn [$messages]
    (d/replace-contents! $messages (messages-list-node @!messages))
    (add-watch !messages ::binder
               (fn [_ _ _ messages]
                 (d/replace-contents! $messages (messages-list-node messages))))))

(defn messages-widget [!messages sent-messages-ch]
  (node
   [:div {:style {:margin "2em"}}
    (messages-box-node (messages-binder !messages))
    (textbox-node (textbox-binder sent-messages-ch))]))
