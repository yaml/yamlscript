;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.http
  (:require
   [babashka.http-client :as http])
  (:refer-clojure :exclude [get]))

(def this 'ys.http)

(intern this 'delete http/delete)
(intern this 'get http/get)
(intern this 'head http/head)
(intern this 'patch http/patch)
(intern this 'post http/post)
(intern this 'put http/put)

(comment
  )
