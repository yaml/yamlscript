//! Rust binding/API for the libyamlscript shared library.
//!
//! # Loading a YAMLScript file
//! The [`YAMLScript::load`] function is the main entrypoint of the library.
//! It allows loading YAMLScript and returns a JSON object.
//! `serde_json` is used to deserialize the JSON.
//! One can either use `serde_json::Value` as a return type or a custom `serde::Deserialize`able
//! type.
//!
//! ## Using `serde_json::Value`
//! ```
//! // Create an instance of a YAMLScript object.
//! // This object holds data from the library and all execution context.
//! let ys = yamlscript::YAMLScript::new().unwrap();
//! // Load some YAMLScript.
//! let data = ys.load::<serde_json::Value>(
//!         r#"!yamlscript/v0/data
//!            key: ! inc(42)"#
//!     )
//!     .unwrap();
//!
//! // Our YAMLScript returns a `serde_json::Value` which holds an object.
//! let data = data.as_object().unwrap();
//! // We have a `key` field which holds the value 43.
//! assert_eq!(data.get("key").unwrap().as_u64().unwrap(), 43);
//! ```
//!
//! ## Using a user-defined type
//! ```
//! # use serde::Deserialize;
//! #
//! #[derive(Deserialize)]
//! struct Foo {
//!   key: u64,
//! }
//!
//! // Create an instance of a YAMLScript object.
//! // This object holds data from the library and all execution context.
//! let ys = yamlscript::YAMLScript::new().unwrap();
//! // Load some YAMLScript and deserialize as `Foo`.
//! let foo = ys.load::<Foo>(
//!         r#"!yamlscript/v0/data
//!            key: ! inc(42)"#
//!     )
//!     .unwrap();
//!
//! // Our `foo` object has its key field set to 43.
//! assert_eq!(foo.key, 43);
//! ```

#![warn(clippy::pedantic)]

use std::path::Path;

use dlopen::symbor::Library;
use libc::{c_int, c_void as void};

mod error;

pub use error::Error;
use serde::Deserialize;

use crate::error::LibYAMLScriptError;

/// The name of the yamlscript library to load.
const LIBYAMLSCRIPT_FILENAME: &str = "libyamlscript.so.0.1.50";

/// A wrapper around libyamlscript.
pub struct YAMLScript {
    /// A handle to the opened dynamic library.
    _handle: Library,
    /// A GraalVM isolate.
    _isolate: *mut void,
    /// A GraalVM isolate thread.
    isolate_thread: *mut void,
    /// Pointer to the function in GraalVM to create the isolate and its thread.
    _create_isolate_fn: CreateIsolateFn,
    /// Pointer to the function in GraalVM to free an isolate thread.
    tear_down_isolate_fn: TearDownIsolateFn,
    /// Pointer to the `load_ys_to_json` function in libyamlscript.
    load_ys_to_json_fn: LoadYsToJsonFn,
}

/// Prototype of the `graal_create_isolate` function.
type CreateIsolateFn = unsafe extern "C" fn(*mut void, *const *mut void, *const *mut void) -> c_int;
/// Prototype of the `graal_tear_down_isolate` function.
type TearDownIsolateFn = unsafe extern "C" fn(*mut void) -> c_int;
/// Prototype of the `load_ys_to_json` function.
type LoadYsToJsonFn = unsafe extern "C" fn(*mut void, *const u8) -> *mut i8;

impl YAMLScript {
    /// Create a new instance of a YAMLScript loader.
    ///
    /// # Errors
    /// This function may return an error if we fail to open the library
    /// Namely, it returns [`Error::NotFound`] if the library cannot be found.
    #[allow(clippy::crosspointer_transmute)]
    pub fn new() -> Result<Self, Error> {
        // Open library and create pointers the library needs.
        let handle = Self::open_library()?;
        let isolate = std::ptr::null_mut();
        let isolate_thread = std::ptr::null_mut();

        // Fetch symbols.
        let create_isolate_fn =
            unsafe { handle.ptr_or_null::<CreateIsolateFn>("graal_create_isolate")? };
        let tear_down_isolate_fn =
            unsafe { handle.ptr_or_null::<TearDownIsolateFn>("graal_tear_down_isolate")? };
        let load_ys_to_json_fn =
            unsafe { handle.ptr_or_null::<LoadYsToJsonFn>("load_ys_to_json")? };

        // Check for null-ness.
        if create_isolate_fn.is_null()
            || tear_down_isolate_fn.is_null()
            || load_ys_to_json_fn.is_null()
        {
            return Err(Error::Load(dlopen::Error::NullSymbol));
        }

        // We are doing 2 things here:
        //   1. Remove the borrow of the function pointers on the `Library`.
        //   2. Convert them to the correct Rust type.
        //
        // 1. By copying the pointers, we remove the borrow `PtrOrNull` has on `Library`.
        //    Without this, we would be unable to move `handle` to return our `Self` at the end of
        //    this function, because the `Library` would be moved while still borrowed.
        //    We have checked the pointers to be valid and they are stored alongside the `Library`
        //    and hence do not outlive it.
        // 2. Rust considers that the type `fn()` is akin to a function pointer.
        //    When we retrieve a `*const fn()`, this is thus a pointer to a function pointer.
        //    We need to transmute the pointer to pointer to a regular pointer.
        //    This means converting a pointer to its pointee, which clippy does not like at all,
        //    but it is valid in this context.
        //    Note that we dereference (`*`) the `_fn` bindings to retrieve the raw pointer from
        //    the `PtrOrNull` wrapper
        //    There is no pointer dereferencement here.
        let create_isolate_fn: CreateIsolateFn = unsafe { std::mem::transmute(*create_isolate_fn) };
        let tear_down_isolate_fn: TearDownIsolateFn =
            unsafe { std::mem::transmute(*tear_down_isolate_fn) };
        let load_ys_to_json_fn: LoadYsToJsonFn =
            unsafe { std::mem::transmute(*load_ys_to_json_fn) };

        let x = unsafe { (create_isolate_fn)(std::ptr::null_mut(), &isolate, &isolate_thread) };
        if x != 0 {
            return Err(Error::GraalVM(x));
        }

        Ok(Self {
            _handle: handle,
            _isolate: isolate,
            isolate_thread,
            _create_isolate_fn: create_isolate_fn,
            tear_down_isolate_fn,
            load_ys_to_json_fn,
        })
    }

    /// Load a YAMLScript string, returning the result deserialized.
    ///
    /// # Errors
    /// This function returns an error if the input string is invalid (contains a nil-byte) or if
    /// the YAMLScript engine has returned an error.
    pub fn load<T>(&self, ys: &str) -> Result<T, Error>
    where
        T: serde::de::DeserializeOwned,
    {
        // Library responds with a JSON string.
        // Parse it.
        let raw = unsafe { std::ffi::CStr::from_ptr(self.load_raw(ys, self.isolate_thread)?) }
            .to_str()?;
        let response = serde_json::from_str::<YsResponse<T>>(raw)?;

        // Check for errors.
        match response {
            YsResponse::Data(value) => Ok(value),
            YsResponse::Error(err) => Err(Error::YAMLScript(err)),
        }
    }

    /// Load a YAMLScript string, returning the raw buffer from the library.
    ///
    /// # Errors
    /// This function returns an error if the input string is invalid (contains a nil-byte).
    ///
    /// If the yamlscript engine returned an error, this function will succeed.
    fn load_raw(&self, ys: &str, isolate_thread: *mut void) -> Result<*mut i8, Error> {
        // We need to create a `CString` because `ys` is not necessarily nil-terminated.
        let input = std::ffi::CString::new(ys)
            .map_err(|_| Error::Ffi("load: input contains a nil-byte".to_string()))?;
        let json = unsafe { (self.load_ys_to_json_fn)(isolate_thread, input.as_bytes().as_ptr()) };
        if json.is_null() {
            Err(Error::Ffi(
                "load_ys_to_json: returned a null pointer".to_string(),
            ))
        } else {
            Ok(json)
        }
    }

    /// Open the library found at the first matching path in `LD_LIBRARY_PATH`.
    fn open_library() -> Result<Library, Error> {
        let mut first_error = None;
        let library_path = std::env::var("LD_LIBRARY_PATH").map_err(|_| Error::NotFound)?;

        // Additionally look in `/usr/local/lib` and `${HOME}/.local/lib`.
        let mut additional_paths = vec!["/usr/local/lib"];
        let home_path = std::env::var("HOME")
            .ok()
            .map(|home| format!("{home}/.local/lib"));
        if let Some(path) = &home_path {
            additional_paths.push(path.as_str());
        }

        // Iterate over segments of `LD_LIBRARY_PATH`.
        for path in library_path.split(':').chain(additional_paths.into_iter()) {
            // Try to open the library, if it exists.
            let path = Path::new(path).join(LIBYAMLSCRIPT_FILENAME);
            if !path.is_file() {
                continue;
            }
            let library = Library::open(path);

            // Store the error that happened if we haven't encountered one.
            // The error we store always happened while trying to open an existing
            // `libyamlscript.so` file.
            // It should be helpful.
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
            None => Err(Error::NotFound),
        }
    }
}

impl Drop for YAMLScript {
    fn drop(&mut self) {
        let res = unsafe { (self.tear_down_isolate_fn)(self.isolate_thread) };
        if res != 0 {
            eprintln!("Warning: Failed to tear down yamlscript's GraalVM isolate");
        }
    }
}

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
///   "error": {
///     // An error object (see [`LibYAMLScriptError`]).
///   }
/// }
/// ```
///
/// Upon success, we can directly deserialize the `data` object into the target type.
/// Upon error, we deserialize the error as [`LibYAMLScriptError`] so that it can be inspected.
/// We however do not provide any inspection helpers.
#[derive(Deserialize)]
enum YsResponse<T> {
    /// A JSON object containing the result of evaluating the yamlscript.
    #[serde(rename = "data")]
    Data(T),
    /// An error object.
    #[serde(rename = "error")]
    Error(LibYAMLScriptError),
}
