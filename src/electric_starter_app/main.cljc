(ns electric-starter-app.main
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]))

(e/defn PaginationButtons [!page-number]
  (e/client
    (dom/div
      (dom/button
        (dom/On "click" (fn [_] (reset! !page-number 1)) nil)
        (dom/text "<< First Page"))

      (dom/button
        (dom/On "click" (fn [_] (swap! !page-number dec)) nil)
        (dom/text "Prev Page"))

      (dom/button
        (dom/On "click" (fn [_] (swap! !page-number inc)) nil)
        (dom/text "Next Page"))

      (dom/button
        (dom/On "click" (fn [_] (reset! !page-number 1)) nil)
        (dom/text "Last Page >>")))))

(e/defn UserList []
  (e/client
    (let [!page-number  (atom 1)
          page-number   (e/watch !page-number)
          page-size     5]
      (e/server
        (let [offset      (* page-size (dec page-number))
              limit       page-size

              ; this just makes 1000 plain maps
              users       (for [x (range 1000)]
                            {:db/id   x
                             :eacl/id (str "id-" x)}) ; (lookup-users-with-accounts db acl) ;(e/Offload #(lookup-users-with-accounts db acl)) ; (qry-user-ids db) ; or use lookup-users-with-accounts
              sorted      (->> users (sort-by :db/id))
              paginated   (->> sorted (drop offset) (take limit)) ; also crashes if you call (vec) on list.
              diffed-page (e/diff-by :db/id paginated)] ; also crashes if you pass identity

          (e/client
            (dom/h2 (dom/text (e/server (count sorted)) " Users"))

            (dom/text "Click on Next Page 3 times to crash Electric:")
            (PaginationButtons !page-number)

            (dom/ul
              (e/server ; change to e/client to not crash. when run in e/server, the e/for crashes on third page, regardless of page size.
                (e/for [user diffed-page]
                  (e/client
                    (dom/li (dom/text "User " (e/server (pr-str user))))))))))))))

(e/defn Main [ring-request]
  (e/server
    (e/client
      (binding [dom/node js/document.body]                  ; DOM nodes will mount under this one
        (dom/div                                            ; mandatory wrapper div to ensure node ordering - https://github.com/hyperfiddle/electric/issues/74

          (dom/h1 (dom/text "Reproduce Electric e/for in e/server bug"))

          (dom/div
            (UserList)))))))

(defn electric-boot [ring-request]
  #?(:clj  (e/boot-server {} Main (e/server ring-request))  ; inject server-only ring-request
     :cljs (e/boot-client {} Main (e/server (e/amb)))))     ; symmetric – same arity – no-value hole in place of server-only ring-request
