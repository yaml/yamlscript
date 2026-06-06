-- Copyright 2022-2025 Ingy döt Net
-- This code is licensed under MIT license (See License for details)

{-# LANGUAGE ForeignFunctionInterface #-}
{-# LANGUAGE CApiFFI #-}
{-# LANGUAGE ScopedTypeVariables #-}

module YAMLScript.FFI
  ( loadYsToJsonFFI
  ) where

import Foreign
import Foreign.C.String
import Foreign.C.Types
import qualified Data.ByteString as BS
import qualified Data.ByteString.Unsafe as BSU
import Control.Exception (bracket)

-- | GraalVM isolate thread type
data GraalCreateIsolateParams
data GraalIsolate
data GraalIsolateThread

type GraalIsolateThreadPtr = Ptr GraalIsolateThread

-- | Foreign function interface to GraalVM isolate management
foreign import capi "graal_isolate.h graal_create_isolate"
  c_graal_create_isolate
    :: Ptr GraalCreateIsolateParams
    -> Ptr (Ptr GraalIsolate)
    -> Ptr GraalIsolateThreadPtr
    -> IO CInt

foreign import capi "graal_isolate.h graal_tear_down_isolate"
  c_graal_tear_down_isolate :: GraalIsolateThreadPtr -> IO CInt

-- | Foreign function interface to load_ys_to_json
foreign import capi "libys.0.2.12.h load_ys_to_json"
  c_load_ys_to_json :: CLLong -> CString -> IO CString

-- | Create a GraalVM isolate and run an action with it
withGraalIsolate :: (GraalIsolateThreadPtr -> IO a) -> IO a
withGraalIsolate action =
  alloca $ \(isolateThreadPtr :: Ptr GraalIsolateThreadPtr) -> do
    -- Create the isolate (following Python binding pattern:
    -- None, None, &isolatethread).
    rc <- c_graal_create_isolate nullPtr nullPtr isolateThreadPtr
    if rc /= 0
      then error $ "Failed to create GraalVM isolate (code " ++ show rc ++ ")"
      else do
        isolateThread <- peek isolateThreadPtr
        -- Run the action and ensure cleanup
        bracket
          (return isolateThread)
          (\thread -> do
            rc' <- c_graal_tear_down_isolate thread
            if rc' /= 0
              then
                error $
                  "Failed to tear down GraalVM isolate (code " ++
                  show rc' ++ ")"
              else return ())
          action

-- | Load YAMLScript code and return JSON result
loadYsToJsonFFI :: BS.ByteString -> IO BS.ByteString
loadYsToJsonFFI input =
  withGraalIsolate $ \isolateThread ->
    BSU.unsafeUseAsCString input $ \cInput -> do
      cResult <- c_load_ys_to_json (threadId isolateThread) cInput
      if cResult == nullPtr
        then return BS.empty
        else BS.packCString cResult

threadId :: GraalIsolateThreadPtr -> CLLong
threadId = fromIntegral . ptrToIntPtr
