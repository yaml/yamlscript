#![warn(clippy::pedantic)]

use std::{path::Path, sync::OnceLock};

use dlopen::symbor::Library;
use libc::{c_int, c_void as void};

mod error;

pub use error::Error;
use serde::Deserialize;

use crate::error::LibInitError;

/// A response from the yamlscript library.
///
/// The library reports success with an object resembling:
/// ```json
/// {
///   "data": {
///     // JSON value after evaluating the yamlscript
///   }
/// }
/// ```
///
/// The library reports failure with an object resembling:
/// ```json
/// {
///   "error: {
///     // An object we leave as-is.
///   }
/// }
/// ```
///
/// Upon success, we can directly deserialize the `data` object into the target type.
/// Upon error, we keep an opaque [`serde_json::Value`] object so as to not further depend on
/// specifics of `libyamlscript.so`.
#[derive(Deserialize)]
struct YsResponse<T> {
    /// If present, a JSON object containing the result of evaluating the yamlscript.
    data: Option<T>,
    /// If present, an error object.
    error: Option<serde_json::Value>,
}

/// The name of the yamlscript library to load.
const LIBYAMLSCRIPT_FILENAME: &str = "libyamlscript.so.0.1.34";

/// Load a YS string, returning the result deserialized.
///
/// # Errors
/// This function returns an error if the library was not correctly loaded, if the input string is
/// invalid (contains a nil-byte) or if the Yamlscript engine has returned an error.
pub fn load<T>(ys: &str) -> Result<T, Error>
where
    T: serde::de::DeserializeOwned,
{
    // Library responds with a JSON string. Parse it.
    let raw = unsafe { std::ffi::CStr::from_ptr(load_raw(ys)?) }.to_str()?;
    let response = serde_json::from_str::<YsResponse<T>>(raw)?;

    // Check for errors.
    if let Some(err) = response.error {
        return Err(Error::Yamlscript(err));
    }

    if let Some(value) = response.data {
        Ok(value)
    } else {
        Err(Error::Ffi(format!(
            "load: no `data` or `error` field: {raw}"
        )))
    }
}

/// Load a YS string, returning the raw buffer from the library.
///
/// # Errors
/// This function returns an error if the library was not correctly loaded, if the input string is
/// invalid (contains a nil-byte).
///
/// If the yamlscript engine returned an error, this function will succeed.
fn load_raw(ys: &str) -> Result<*mut i8, Error> {
    LibYamlscript::with_library(|yamlscript| {
        // We need to create a `CString` because `ys` is not necessarily nil-terminated.
        let input = std::ffi::CString::new(ys)
            .map_err(|_| Error::Ffi("load: input contains a nil-byte".to_string()))?;
        let json = unsafe {
            (yamlscript.load_ys_to_json_fn)(
                yamlscript.isolate_thread as libc::c_longlong,
                input.as_bytes().as_ptr(),
            )
        };
        if json.is_null() {
            Err(Error::Ffi(
                "load_ys_to_json: returned a null pointer".to_string(),
            ))
        } else {
            Ok(json)
        }
    })?
}

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
    /// Pointer to the `load_ys_to_json` function in libyamlscript.
    load_ys_to_json_fn: LoadYsToJsonFn,
    /// Pointer to the `compile_ys_to_clj` function in libyamlscript.
    compile_ys_to_clj_fn: CompileYsToClj,
}

unsafe impl Send for LibYamlscript {}
unsafe impl Sync for LibYamlscript {}

/// Prototype of the `graal_create_isolate` function.
type CreateIsolateFn = unsafe extern "C" fn(*mut void, *const *mut void, *const *mut void) -> c_int;
/// Prototype of the `load_ys_to_json` function.
type LoadYsToJsonFn = unsafe extern "C" fn(libc::c_longlong, *const u8) -> *mut i8;
/// Prototype of the `compile_ys_to_clj` function.
type CompileYsToClj = unsafe extern "C" fn(libc::c_longlong, *const u8) -> *mut i8;

impl LibYamlscript {
    /// Find and open the `libyamlscript` file and load functions into memory.
    #[allow(clippy::crosspointer_transmute)]
    fn load() -> Result<Self, LibInitError> {
        // Open library and create pointers the library needs.
        let handle = Self::open_library()?;
        let isolate = std::ptr::null_mut();
        let isolate_thread = std::ptr::null_mut();

        // Fetch symbols.
        let create_isolate_fn =
            unsafe { handle.ptr_or_null::<CreateIsolateFn>("graal_create_isolate")? };
        let load_ys_to_json_fn =
            unsafe { handle.ptr_or_null::<LoadYsToJsonFn>("load_ys_to_json")? };
        let compile_ys_to_clj_fn =
            unsafe { handle.ptr_or_null::<CompileYsToClj>("compile_ys_to_clj")? };

        // Check for null-ness.
        if create_isolate_fn.is_null()
            || load_ys_to_json_fn.is_null()
            || compile_ys_to_clj_fn.is_null()
        {
            return Err(LibInitError::Load(dlopen::Error::NullSymbol));
        }

        // We are doing 2 things here:
        //   1. Remove the borrow of the function pointers on the `Library`.
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
        //    the `PtrOrNull` wrapper. There is no pointer dereferencement here.
        let create_isolate_fn: CreateIsolateFn = unsafe { std::mem::transmute(*create_isolate_fn) };
        let load_ys_to_json_fn: LoadYsToJsonFn =
            unsafe { std::mem::transmute(*load_ys_to_json_fn) };
        let compile_ys_to_clj_fn: CompileYsToClj =
            unsafe { std::mem::transmute(*compile_ys_to_clj_fn) };

        // Initialize the thread.
        let x = unsafe { create_isolate_fn(std::ptr::null_mut(), &isolate, &isolate_thread) };
        if x != 0 {
            return Err(LibInitError::Init(x));
        }

        Ok(Self {
            handle,
            isolate,
            isolate_thread,
            create_isolate_fn,
            load_ys_to_json_fn,
            compile_ys_to_clj_fn,
        })
    }

    /// Execute a callback with an instance of the library.
    fn with_library<T, F: FnOnce(&Self) -> T>(f: F) -> Result<T, Error> {
        static CELL: OnceLock<Result<LibYamlscript, LibInitError>> = OnceLock::new();
        match CELL.get_or_init(Self::load) {
            Ok(yaml_lib) => Ok(f(yaml_lib)),
            Err(e) => Err(e.clone().into()),
        }
    }

    /// Open the library found at the first matching path in `LD_LIBRARY_PATH`.
    fn open_library() -> Result<Library, LibInitError> {
        let mut first_error = None;
        let library_path = std::env::var("LD_LIBRARY_PATH").map_err(|_| LibInitError::NotFound)?;

        // Iterate over segments of `LD_LIBRARY_PATH`.
        for path in library_path
            .split(':')
            .chain(std::iter::once("/usr/local/lib"))
        {
            // Try to open the library, if it exists.
            let path = Path::new(path).join(LIBYAMLSCRIPT_FILENAME);
            if !path.is_file() {
                continue;
            }
            let library = Library::open(path);

            // Store the error that happened if we haven't encountered one.
            // The error we store always happened while trying to open an existing
            // `libyamlscript.so` file. It should be helpful.
            match library {
                Ok(x) => return Ok(x),
                Err(x) => {
                    if first_error.is_none() {
                        first_error = Some(x);
                    }
                }
            }
        }

        // If `first_error` wasn't assigned, we found no matching path.
        match first_error {
            Some(x) => Err(x.into()),
            None => Err(LibInitError::NotFound),
        }
    }
}
