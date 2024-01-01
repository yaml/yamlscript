import YAMLScript (loadYS)

main :: IO ()
main = do
  putStrLn "mdr"
  txt <- loadYS "inc: 4"
  putStrLn txt
  putStrLn "boh"
