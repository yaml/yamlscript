-- Copyright 2022-2026 Ingy döt Net
-- This code is licensed under MIT license (See License for details)

{-# LANGUAGE ForeignFunctionInterface #-}
{-# LANGUAGE OverloadedStrings #-}

module YAMLScript
  ( loadYAMLScript
  , loadYAMLScriptFile
  , YAMLScriptError(..)
  ) where

import qualified Data.ByteString as BS
import qualified Data.ByteString.Lazy as LBS
import qualified Data.Text as T
import qualified Data.Text.Encoding as TE
import qualified Data.Aeson as Aeson
import qualified Data.Aeson.Types as Aeson
import Control.Exception (Exception, throwIO)
import Control.Monad.IO.Class (MonadIO, liftIO)
import System.IO.Unsafe (unsafePerformIO)
import YAMLScript.FFI

-- | Error type for YAMLScript operations
data YAMLScriptError
  = YAMLScriptParseError String
  | YAMLScriptRuntimeError String
  | YAMLScriptFFIError String
  deriving (Show, Eq)

instance Exception YAMLScriptError

-- | Load and evaluate YAMLScript code from a string
-- Returns the result as a JSON Value
loadYAMLScript :: MonadIO m => T.Text -> m Aeson.Value
loadYAMLScript input = liftIO $ do
  let inputBS = TE.encodeUtf8 input
  result <- loadYsToJsonFFI inputBS
  case Aeson.eitherDecode (LBS.fromStrict result) of
    Left err -> throwIO $ YAMLScriptParseError err
    Right value -> return value

-- | Load and evaluate YAMLScript code from a file
-- Returns the result as a JSON Value
loadYAMLScriptFile :: MonadIO m => FilePath -> m Aeson.Value
loadYAMLScriptFile filepath = liftIO $ do
  content <- BS.readFile filepath
  result <- loadYsToJsonFFI content
  case Aeson.eitherDecode (LBS.fromStrict result) of
    Left err -> throwIO $ YAMLScriptParseError err
    Right value -> return value

-- | Convenience function for pure contexts
-- Note: This uses unsafePerformIO and should be used carefully
loadYAMLScriptPure :: T.Text -> Aeson.Value
loadYAMLScriptPure = unsafePerformIO . loadYAMLScript
