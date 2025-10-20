-- Copyright 2022-2025 Ingy döt Net
-- This code is licensed under MIT license (See License for details)

{-# LANGUAGE OverloadedStrings #-}

module YAMLScript.Tests where

import Test.Hspec
import qualified Data.Text as T
import qualified Data.Aeson as Aeson
import qualified Data.Aeson.Key as Key
import qualified Data.Aeson.KeyMap as KeyMap
import qualified Data.Vector as V
import YAMLScript

-- | Test basic YAML functionality
basicTests :: Spec
basicTests = describe "Basic YAML" $ do
  it "parses simple key-value pairs" $ do
    result <- loadYAMLScript "key: value"
    result `shouldBe` Aeson.object
      [("data", Aeson.object [("key", Aeson.String "value")])]

  it "parses lists" $ do
    result <- loadYAMLScript "list: [1, 2, 3]"
    result `shouldBe` Aeson.object
      [ ("data", Aeson.object
          [ ("list", Aeson.Array $ V.fromList
              [Aeson.Number 1, Aeson.Number 2, Aeson.Number 3])
          ])
      ]

  it "parses nested objects" $ do
    result <- loadYAMLScript "nested:\n  key: value"
    result `shouldBe` Aeson.object
      [ ("data", Aeson.object
          [ ("nested", Aeson.object [("key", Aeson.String "value")])
          ])
      ]

-- | Test YAMLScript functionality
yamlscriptTests :: Spec
yamlscriptTests = describe "YAMLScript Features" $ do
  it "evaluates arithmetic expressions" $ do
    result <- loadYAMLScript "!ys-0\nadd(2, 3)"
    result `shouldBe` Aeson.object [("data", Aeson.Number 5)]

  it "evaluates function calls" $ do
    result <- loadYAMLScript "!ys-0\ninc(41)"
    result `shouldBe` Aeson.object [("data", Aeson.Number 42)]

-- | Test error handling
errorTests :: Spec
errorTests = describe "Error Handling" $ do
  it "returns error on invalid YAMLScript" $ do
    result <- loadYAMLScript "!ys-0\ninvalid: syntax"
    case result of
      Aeson.Object obj -> case KeyMap.lookup (Key.fromString "error") obj of
        Just _ -> return () -- Error present, test passes
        Nothing -> expectationFailure "Expected error response"
      _ -> expectationFailure "Expected JSON object"

  it "throws on missing file" $ do
    (loadYAMLScriptFile "nonexistent.ys") `shouldThrow` anyException
