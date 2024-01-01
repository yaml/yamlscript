use std::{str::Utf8Error, sync::Arc};

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
    /// An error while initializing the library.
    ///
    /// The library has been correctly found and opened, but attempting to initialize it has
    /// failed.
    Init(i32),
    /// An error in the FFI while calling a libyamlscript function.
    Ffi(String),
    /// An error from the libyamlscript library.
    ///
    /// This variant is used when we have successfully resolved the function we want to call in
    /// `libyamlscript.so`, but the engine returned an error, that we successfully parsed.
    Yamlscript(serde_json::Value),
    /// An error with serde_json while deserializing.
    Serde(Arc<serde_json::Error>),
    /// An error while decoding strings returned from libyamlscript.
    Utf8(Utf8Error),
}

impl From<dlopen::Error> for Error {
    fn from(value: dlopen::Error) -> Self {
        Self::Load(value)
    }
}

impl From<serde_json::Error> for Error {
    fn from(value: serde_json::Error) -> Self {
        Self::Serde(Arc::new(value))
    }
}

impl From<Utf8Error> for Error {
    fn from(value: Utf8Error) -> Self {
        Self::Utf8(value)
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

impl Clone for Error {
    fn clone(&self) -> Self {
        match self {
            Self::Load(x) => Self::Load(clone_dl_error(x)),
            Self::Init(x) => Self::Init(*x),
            Self::Ffi(x) => Self::Ffi(x.clone()),
            Self::Yamlscript(x) => Self::Yamlscript(x.clone()),
            Self::NotFound => Self::NotFound,
            Self::Serde(x) => Self::Serde(x.clone()),
            Self::Utf8(x) => Self::Utf8(*x),
        }
    }
}
