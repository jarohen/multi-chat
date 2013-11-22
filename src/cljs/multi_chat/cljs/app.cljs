(ns multi-chat.cljs.app
  (:require [cljs.core.async :as a]
            clojure.browser.repl)
  (:require-macros [dommy.macros :refer [node sel1]]))

(set! (.-onload js/window)
      (fn []
        (let [$content (sel1 :#content)]
          (d/replace-contents! $content (node [:h2 "ClojureScript Chat:"])))))


