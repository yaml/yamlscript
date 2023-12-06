use std::sync::OnceLock;

use dlopen::symbor::Library;
use libc::{c_int, c_void as void};

/// A wrapper around libyamlscript.
#[allow(dead_code)]
struct LibYamlscript {
    /// A handle to the opened dynamic library.
    handle: Library,
    /// A GraalVM isolate.
    isolate: *mut void,
    /// A GrallVM thread attached to the isolate.
    isolate_thread: *mut void,
    /// Pointer to the function in GraalVM to create the isolate and its thread.
    create_isolate_fn: CreateIsolateFn,
    /// Pointer to the `eval_ys_to_json` function in libyamlscript.
    eval_ys_to_json_fn: EvalYsToJsonFn,
}

unsafe impl Send for LibYamlscript {}
unsafe impl Sync for LibYamlscript {}

/// An error with libyamlscript.
#[derive(Debug)]
pub enum Error {
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
            Self::Yamlscript(x) => Self::Yamlscript(x.clone()),
        }
    }
}

/// Prototype of the `graal_create_isolate` function.
type CreateIsolateFn = unsafe extern "C" fn(*mut void, *const *mut void, *const *mut void) -> c_int;
/// Prototype of the `eval_ys_to_json` function.
type EvalYsToJsonFn = unsafe extern "C" fn(libc::c_longlong, *const u8) -> *mut i8;

impl LibYamlscript {
    /// Find and open the `libyamlscript` file and load functions into memory.
    #[allow(clippy::crosspointer_transmute)]
    fn load() -> Result<Self, Error> {
        const PATH: &str = "..//libyamlscript/lib/libyamlscript.so.0.1.20";

        // Open library and create pointers the library needs.
        let handle = Library::open(PATH).map_err(Error::Load)?;
        let isolate = std::ptr::null_mut();
        let isolate_thread = std::ptr::null_mut();

        // Fetch symbols
        let create_isolate_fn =
            unsafe { handle.ptr_or_null::<CreateIsolateFn>("graal_create_isolate")? };
        let eval_ys_to_json_fn =
            unsafe { handle.ptr_or_null::<EvalYsToJsonFn>("eval_ys_to_json")? };

        // Check for null-ness.
        if create_isolate_fn.is_null() || eval_ys_to_json_fn.is_null() {
            return Err(Error::Load(dlopen::Error::NullSymbol));
        }

        // We are doing 2 things here:
        //   1. Remove the borrow of the function pointers on the `Library`
        //   2. Convert them to the correct Rust type.
        //
        // 1. By copying the pointers, we remove the borrow `PtrOrNull` has on `Library`. Without
        //    this, we would be unable to move `handle` to return our `Self` at the end of this
        //    function, because the `Library` would be moved while still borrowed.
        //    We have checked the pointers to be valid and they are stored alongside the `Library`
        //    and hence do not outlive it.
        // 2. Rust considers that the type `fn()` is akin to a function pointer. When we retrieve a
        //    `*const fn()`, this is thus a pointer to a function pointer. We need to transmute the
        //    pointer to pointer to a regular pointer. This means converting a pointer to its
        //    pointee, which clippy does not like at all, but it is valid in this context.
        //    Note that we dereference (`*`) the `_fn` bindings to retrieve the raw pointer from
        //    the `PtrOrNull` struct. There is no pointer dereferencement here.
        let create_isolate_fn: CreateIsolateFn = unsafe { std::mem::transmute(*create_isolate_fn) };
        let eval_ys_to_json_fn: EvalYsToJsonFn =
            unsafe { std::mem::transmute(*eval_ys_to_json_fn) };

        // Initialize the thread
        let x = unsafe { create_isolate_fn(std::ptr::null_mut(), &isolate, &isolate_thread) };
        if x != 0 {
            return Err(Error::Init(x));
        }

        Ok(Self {
            handle,
            isolate,
            isolate_thread,
            create_isolate_fn,
            eval_ys_to_json_fn,
        })
    }

    /// Execute a callback with an instance of the library.
    fn with_library<T, F: FnOnce(&Self) -> T>(f: F) -> Result<T, Error> {
        static CELL: OnceLock<Result<LibYamlscript, Error>> = OnceLock::new();
        match CELL.get_or_init(Self::load) {
            Ok(yaml_lib) => Ok(f(yaml_lib)),
            Err(e) => Err(e.clone()),
        }
    }

    /// Evaluate a YS string, returning a JSON string.
    pub fn eval_ys_to_json(ys: &str) -> Result<String, Error> {
        Self::with_library(|yamlscript| {
            // We need to create a `CString` because `ys` is not necessarily nil-terminated.
            let input = std::ffi::CString::new(ys).map_err(|_| {
                Error::Yamlscript("eval_ys_to_json: input contains a nil-byte".to_string())
            })?;
            let json = unsafe {
                (yamlscript.eval_ys_to_json_fn)(
                    yamlscript.isolate_thread as libc::c_longlong,
                    input.as_bytes().as_ptr(),
                )
            };
            if json.is_null() {
                Err(Error::Yamlscript(
                    "eval_ys_to_json: returned null pointer".to_string(),
                ))
            } else {
                let json = unsafe { std::ffi::CStr::from_ptr(json) };
                Ok(json.to_string_lossy().to_string())
            }
        })?
    }
}

fn main() {
    dbg!(LibYamlscript::eval_ys_to_json("Advent day: 3").unwrap());
}
