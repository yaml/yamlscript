open System
open YAMLScript

[<EntryPoint>]
let main _ =
  use ys = new YAMLScript()
  let data = ys.Load("!ys-0:\ntest:: inc(41)")

  if data.GetProperty("test").GetInt32() <> 42 then
    failwith "load ys code failed"

  printfn "ok - load ys code"
  0
