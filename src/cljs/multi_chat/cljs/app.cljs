(ns multi-chat.cljs.app
  (:require [multi-chat.cljs.widget :refer [messages-widget]]
            [multi-chat.cljs.model :refer [wire-up-messages!]]
            [cljs.core.async :as a]
            [dommy.core :as d]
            clojure.browser.repl)
  (:require-macros [dommy.macros :refer [node sel1]]))

(set! (.-onload js/window)
      (fn []
        (let [$content (sel1 :#content)
              !messages (atom [])
              sent-messages-ch (a/chan)]
          (d/replace-contents! (sel1 :#content)
                               (node
                                [:div.container
                                 [:h2 "ClojureScript Chat:"]
                                 (messages-widget !messages sent-messages-ch)]))
          (wire-up-messages! !messages sent-messages-ch))))




