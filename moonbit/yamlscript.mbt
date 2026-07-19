///|
pub const VERSION = "0.2.29"

///|
pub suberror YAMLScriptError {
  YAMLScriptError(String)
} derive(@debug.Debug)

///|
#borrow(input)
extern "C" fn load_ys_to_json(input : Bytes) -> Bytes = "ys_load_ys_to_json"

///|
pub fn load(input : String) -> Json raise {
  let response = input |> @utf8.encode |> load_ys_to_json |> @utf8.decode
  let json = @json.parse(response)
  match json {
    Object(object) =>
      match object.get("error") {
        Some(Object(error)) =>
          match error.get("cause") {
            Some(String(cause)) => raise YAMLScriptError(cause)
            _ => raise YAMLScriptError("Unknown YAMLScript error")
          }
        _ =>
          match object.get("data") {
            Some(data) => data
            _ => raise YAMLScriptError("Unexpected response from libys")
          }
      }
    _ => raise YAMLScriptError("Unexpected response from libys")
  }
}
