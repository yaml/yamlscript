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
    Yamlscript(LibYamlscriptError),
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
pub struct LibYamlscriptError {
    /// The error message.
    pub cause: String,
    /// The stack trace within libyamlscript.
    pub trace: Vec<(String, String, Option<String>, i64)>,
    /// The internal type of the error.
    #[serde(rename = "type")]
    pub type_: String,
}

impl From<LibInitError> for Error {
    fn from(value: LibInitError) -> Self {
        match value {
            LibInitError::NotFound => Self::NotFound,
            LibInitError::Load(x) => Self::Load(x),
        }
    }
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

// A subset of [`Error`] with variants dedicated to loading `libyamlscript.so`.
//
// This allows us to require that [`LibInitError`] be `Clone`, but not [`Error`].
#[derive(Debug)]
pub(crate) enum LibInitError {
    /// The library was not found.
    NotFound,
    /// An error while loading the library.
    ///
    /// This error is unrecoverable and any further attempt to call any libyamlscript function will
    /// fail.
    Load(dlopen::Error),
}

impl From<dlopen::Error> for LibInitError {
    fn from(value: dlopen::Error) -> Self {
        Self::Load(value)
    }
}

impl Clone for LibInitError {
    fn clone(&self) -> Self {
        match self {
            Self::NotFound => Self::NotFound,
            Self::Load(x) => Self::Load(clone_dl_error(x)),
        }
    }
}

/// Attempt to clone a `std::io::Error`.
///
/// Type information of the underlying error, if any, will be lost.
fn clone_std_io_error(err: &std::io::Error) -> std::io::Error {
    std::io::Error::new(
        err.kind(),
        err.get_ref().map(|e| format!("{e}")).unwrap_or_default(),
    )
}

/// Clone a `dlopen::Error`.
fn clone_dl_error(err: &dlopen::Error) -> dlopen::Error {
    type DErr = dlopen::Error;
    match err {
        DErr::NullCharacter(x) => DErr::NullCharacter(x.clone()),
        DErr::OpeningLibraryError(x) => DErr::OpeningLibraryError(clone_std_io_error(x)),
        DErr::SymbolGettingError(x) => DErr::SymbolGettingError(clone_std_io_error(x)),
        DErr::NullSymbol => DErr::NullSymbol,
        DErr::AddrNotMatchingDll(x) => DErr::AddrNotMatchingDll(clone_std_io_error(x)),
    }
}
