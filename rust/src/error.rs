use std::str::Utf8Error;

use serde::Deserialize;

/// An error with libyamlscript.
#[derive(Debug)]
pub enum Error {
    /// The library was not found.
    NotFound,
    /// An error while loading the library.
    ///
    /// This error is unrecoverable and any further attempt to call any libyamlscript function will
    /// fail.
    Load(dlopen::Error),
    /// An error with GraalVM.
    GraalVM(i32),
    /// An error in the FFI while calling a libyamlscript function.
    Ffi(String),
    /// An error from the libyamlscript library.
    ///
    /// This variant is used when we have successfully resolved the function we want to call in
    /// `libyamlscript.so`, but the engine returned an error, that we successfully parsed.
    YAMLScript(LibYAMLScriptError),
    /// An error with serde_json while deserializing.
    Serde(serde_json::Error),
    /// An error while decoding strings returned from libyamlscript.
    Utf8(Utf8Error),
}

/// An error from libyamlscript.
///
/// This gets returned from libyamlscript functions that were successfully called but the engine
/// returned an error.
#[allow(clippy::module_name_repetitions)]
#[derive(Deserialize, Debug)]
pub struct LibYAMLScriptError {
    /// The error message.
    pub cause: String,
    /// The stack trace within libyamlscript.
    pub trace: Vec<(String, String, Option<String>, i64)>,
    /// The internal type of the error.
    #[serde(rename = "type")]
    pub type_: String,
}

impl From<dlopen::Error> for Error {
    fn from(value: dlopen::Error) -> Self {
        Self::Load(value)
    }
}

impl From<serde_json::Error> for Error {
    fn from(value: serde_json::Error) -> Self {
        Self::Serde(value)
    }
}

impl From<Utf8Error> for Error {
    fn from(value: Utf8Error) -> Self {
        Self::Utf8(value)
    }
}
