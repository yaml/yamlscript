/// An error with libyamlscript.
#[derive(Debug)]
pub enum Error {
    /// The library was not found.
    NotFound,

    /// An error while loading the library.
    ///
    /// This error is unrecoverable and any further attempt to call any
    /// libyamlscript function will fail.
    Load(dlopen::Error),

    /// An error while initializing the library.
    ///
    /// The library has been correctly found and opened, but attempting to
    /// initialize it has failed.
    Init(i32),

    /// An error while calling a libyamlscript function.
    Yamlscript(String),
}

impl From<dlopen::Error> for Error {
    fn from(value: dlopen::Error) -> Self {
        Self::Load(value)
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
        DErr::NullCharacter(x) =>
            DErr::NullCharacter(x.clone()),
        DErr::OpeningLibraryError(x) =>
            DErr::OpeningLibraryError(clone_std_io_error(x)),
        DErr::SymbolGettingError(x) =>
            DErr::SymbolGettingError(clone_std_io_error(x)),
        DErr::NullSymbol =>
            DErr::NullSymbol,
        DErr::AddrNotMatchingDll(x) =>
            DErr::AddrNotMatchingDll(clone_std_io_error(x)),
    }
}

impl Clone for Error {
    fn clone(&self) -> Self {
        match self {
            Self::Load(x) => Self::Load(clone_dl_error(x)),
            Self::Init(x) => Self::Init(*x),
            Self::Yamlscript(x) => Self::Yamlscript(x.clone()),
            Self::NotFound => Self::NotFound,
        }
    }
}
