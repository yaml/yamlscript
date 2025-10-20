-- Copyright 2022-2025 Ingy döt Net
-- This code is licensed under MIT license (See License for details)

{-# LANGUAGE OverloadedStrings #-}

module Main where

import Test.Hspec
import qualified Data.Text as T
import qualified Data.Aeson as Aeson
import qualified Data.Aeson.Key as Key
import qualified Data.Aeson.KeyMap as KeyMap
import YAMLScript
import YAMLScript.Tests

main :: IO ()
main = hspec $ do
  describe "YAMLScript" $ do
    it "loads simple YAML" $ do
      result <- loadYAMLScript "foo: bar"
      result `shouldBe` Aeson.object
        [("data", Aeson.object [("foo", Aeson.String "bar")])]

    it "evaluates YAMLScript function call" $ do
      result <- loadYAMLScript "!ys-0\nadd(40, 2)"
      result `shouldBe` Aeson.object [("data", Aeson.Number 42)]

    it "loads from file" $ do
      result <- loadYAMLScriptFile "test/data/simple.ys"
      result `shouldBe` Aeson.object
        [("data", Aeson.object [("test", Aeson.String "value")])]

    it "handles errors gracefully" $ do
      result <- loadYAMLScript "!ys-0\ninvalid: syntax"
      case result of
        Aeson.Object obj -> case KeyMap.lookup (Key.fromString "error") obj of
          Just _ -> return () -- Error present, test passes
          Nothing -> expectationFailure "Expected error response"
        _ -> expectationFailure "Expected JSON object"
