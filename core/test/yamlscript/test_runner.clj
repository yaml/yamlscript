;; This code is licensed under MIT license (See License for details)
;; Copyright 2023-2024 Ingy dot Net

(ns yamlscript.test-runner
  (:require
   [yamltest.core :as test]
   [yamlscript.builder-test]
   [yamlscript.compiler-test]
   [yamlscript.composer-test]
   [yamlscript.constructor-test]
   [yamlscript.parser-test]
   [yamlscript.printer-test]
   [yamlscript.resolver-test]
   [yamlscript.transformer-test]
   ))

(comment
  ;; Pick a namespace to run tests in (:all will always run all tests):
  (in-ns 'yamlscript.compiler-test)

  ;; eval one of these forms to run tests:
  (test/run :a :v)

  @test/test-nss

  (test/run)

  (test/run 3 2 1 :v)

  (test/run :verbose)
  (test/run :all)
  (test/run :all :verbose :reload)
  (test/run :verbose :reload)
  )
