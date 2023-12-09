use std::{path::Path, sync::OnceLock};

use dlopen::symbor::Library;
use libc::{c_int, c_void as void};

mod error;

pub use error::Error;

/// Evaluate a YS string, returning a JSON string.
pub fn load(ys: &str) -> Result<String, Error> {
    LibYamlscript::with_library(|yamlscript| {
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

/// Evaluate a YS string, discarding the result.
pub fn run(ys: &str) -> Result<(), Error> {
    load(ys).map(|_| ())
}

/// Compile a YS string to CLJ.
pub fn compile(ys: &str) -> Result<String, Error> {
    LibYamlscript::with_library(|yamlscript| {
        // We need to create a `CString` because `ys` is not necessarily nil-terminated.
        let input = std::ffi::CString::new(ys).map_err(|_| {
            Error::Yamlscript("compile_ys_to_clj_fn: input contains a nil-byte".to_string())
        })?;
        let clj = unsafe {
            (yamlscript.compile_ys_to_clj_fn)(
                yamlscript.isolate_thread as libc::c_longlong,
                input.as_bytes().as_ptr(),
            )
        };
        if clj.is_null() {
            Err(Error::Yamlscript(
                "compile_ys_to_clj_fn: returned null pointer".to_string(),
            ))
        } else {
            let clj = unsafe { std::ffi::CStr::from_ptr(clj) };
            Ok(clj.to_string_lossy().to_string())
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
    /// Pointer to the `eval_ys_to_json` function in libyamlscript.
    eval_ys_to_json_fn: EvalYsToJsonFn,
    /// Pointer to the `compile_ys_to_clj` function in libyamlscript.
    compile_ys_to_clj_fn: CompileYsToClj,
}

unsafe impl Send for LibYamlscript {}
unsafe impl Sync for LibYamlscript {}

/// Prototype of the `graal_create_isolate` function.
type CreateIsolateFn = unsafe extern "C" fn(*mut void, *const *mut void, *const *mut void) -> c_int;
/// Prototype of the `eval_ys_to_json` function.
type EvalYsToJsonFn = unsafe extern "C" fn(libc::c_longlong, *const u8) -> *mut i8;
/// Prototype of the `compile_ys_to_clj` function.
type CompileYsToClj = unsafe extern "C" fn(libc::c_longlong, *const u8) -> *mut i8;

impl LibYamlscript {
    /// Find and open the `libyamlscript` file and load functions into memory.
    #[allow(clippy::crosspointer_transmute)]
    fn load() -> Result<Self, Error> {
        // Open library and create pointers the library needs.
        let handle = Self::open_library()?;
        let isolate = std::ptr::null_mut();
        let isolate_thread = std::ptr::null_mut();

        // Fetch symbols.
        let create_isolate_fn =
            unsafe { handle.ptr_or_null::<CreateIsolateFn>("graal_create_isolate")? };
        let eval_ys_to_json_fn =
            unsafe { handle.ptr_or_null::<EvalYsToJsonFn>("eval_ys_to_json")? };
        let compile_ys_to_clj_fn =
            unsafe { handle.ptr_or_null::<CompileYsToClj>("compile_ys_to_clj")? };

        // Check for null-ness.
        if create_isolate_fn.is_null()
            || eval_ys_to_json_fn.is_null()
            || compile_ys_to_clj_fn.is_null()
        {
            return Err(Error::Load(dlopen::Error::NullSymbol));
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
        let eval_ys_to_json_fn: EvalYsToJsonFn =
            unsafe { std::mem::transmute(*eval_ys_to_json_fn) };
        let compile_ys_to_clj_fn: CompileYsToClj =
            unsafe { std::mem::transmute(*compile_ys_to_clj_fn) };

        // Initialize the thread.
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
            compile_ys_to_clj_fn,
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

    /// Open the library found at the first matching path in `LD_LIBRARY_PATH`.
    fn open_library() -> Result<Library, Error> {
        let mut first_error = None;
        let library_path = std::env::var("LD_LIBRARY_PATH").map_err(|_| Error::NotFound)?;

        // Iterate over segments of `LD_LIBRARY_PATH`.
        for path in library_path.split(':') {
            // Try to open the library, if it exists.
            let path = Path::new(path).join("libyamlscript.so");
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
                        first_error = Some(x)
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
