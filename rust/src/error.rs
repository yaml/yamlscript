use std::{fmt::Debug, str::Utf8Error};

use serde::Deserialize;

/// An error with the binding.
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

impl Debug for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::NotFound => write!(
                f,
                "Shared library file 'libyamlscript.so.{0}' not found
Try: curl -sSL yamlscript.org/install | VERSION={0} LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript",
                &super::LIBYAMLSCRIPT_VERSION
            ),
            Error::Load(e) => write!(f, "Error::Load({e:?})"),
            Error::GraalVM(e) => write!(f, "Error::GraalVM({e:?})"),
            Error::Ffi(e) => write!(f, "Error::Ffi({e:?})"),
            Error::YAMLScript(e) => write!(f, "Error::YAMLScript({e:?})"),
            Error::Serde(e) => write!(f, "Error::Serde({e:?})"),
            Error::Utf8(e) => write!(f, "Error::Utf8({e:?})"),
        }
    }
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
