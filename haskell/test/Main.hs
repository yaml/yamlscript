-- Copyright 2022-2026 Ingy döt Net
-- This code is licensed under MIT license (See License for details)

module Main where

import qualified Data.Text as T
import qualified Data.Aeson as Aeson
import qualified Data.Aeson.Encode.Pretty as Aeson
import qualified Data.ByteString.Lazy.Char8 as LBS
import System.Environment (getArgs)
import YAMLScript

main :: IO ()
main = do
  args <- getArgs
  case args of
    [] -> putStrLn "Usage: yamlscript-test <yamlscript-code>"
    (code:_) -> do
      let input = T.pack code
      result <- loadYAMLScript input
      LBS.putStrLn $ Aeson.encodePretty result
