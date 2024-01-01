{-# LANGUAGE ForeignFunctionInterface #-}

module YAMLScript (loadYS) where

import qualified Data.Aeson as Aeson
import qualified Data.Text as T
import Data.Int
import Data.Word
import Foreign.C
import Foreign.C.String
import Foreign.StablePtr
import Foreign.Ptr
import Foreign.Storable

foreign import ccall "graal_create_isolate"
  graal_create_isolate :: StablePtr (Ptr Word64) -> StablePtr (Ptr Word64) -> StablePtr (Ptr Word64) -> IO Int
createIsolate :: IO (Ptr Word64)
createIsolate = do
  a <- newStablePtr nullPtr
  t <- newStablePtr nullPtr
  c <- newStablePtr nullPtr
  putStrLn "xd"
  ret <- graal_create_isolate a t c
  putStrLn $ "returned: " ++ (show ret)
  putStrLn "e_e"
  deRefStablePtr c

foreign import ccall "load_ys_to_json"
  load_ys_to_json :: Ptr Word64 -> CString -> IO CString

loadYS :: String -> IO String -- Aeson.Value
loadYS ys = withCString ys $ \ysC -> do
  putStrLn "hihi"
  thread <- createIsolate
  putStrLn "hoho"
  putStrLn $ show thread
  ret <- load_ys_to_json thread ysC
  peekCString ret
