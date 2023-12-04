use dlopen::symbor::Library;

fn main() {
    const PATH: &str = "..//libyamlscript/lib/libyamlscript.so.0.1.20";

    let lib = Library::open(PATH).unwrap();
    let fun = unsafe {
        lib.symbol::<unsafe extern "C" fn(libc::c_longlong, *const u8) -> *mut i8>(
            "eval_ys_to_json",
        )
    }
    .unwrap();
    let isolate: *mut libc::c_void = std::ptr::null_mut();
    let isolate_thread: *mut libc::c_void = std::ptr::null_mut();
    let isolate_fn = unsafe {
        lib.symbol::<unsafe extern "C" fn(
            *mut libc::c_void,
            *const *mut libc::c_void,
            *const *mut libc::c_void,
        ) -> libc::c_int>("graal_create_isolate")
    }
    .unwrap();
    assert_eq!(
        unsafe { isolate_fn(std::ptr::null_mut(), &isolate, &isolate_thread) },
        0
    );
    let s = std::ffi::CString::new("Advent day: 3").unwrap();
    let bytes = s.as_bytes();
    let json = unsafe {
        dbg!(bytes.as_ptr());
        dbg!(isolate_thread);
        dbg!(isolate_thread as libc::c_longlong);
        let json = fun(isolate_thread as libc::c_longlong, bytes.as_ptr());
        if json.is_null() {
            panic!("Err");
        } else {
            let json = std::ffi::CStr::from_ptr(json);
            json.to_string_lossy()
        }
    };
    dbg!(json);
}
