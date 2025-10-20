package yamlscript

// #cgo LDFLAGS: -lys.0.2.5
// #include <libys.0.2.5.h>
// #include <stdlib.h>
import "C"
import (
	"encoding/json"
	"errors"
	"log"
	"runtime"
	"unsafe"
)

// Create a new GraalVM isolate for life of the package
var isolate *C.graal_isolate_t

// YAMLScript package constructor
func init() {
	// Create the isolate without creating an initial thread
	// We'll attach threads as needed per-call
	rc := C.graal_create_isolate(nil, &isolate, nil)
	if rc != 0 {
		log.Fatal("Failed to create isolate")
	}
}

// Compile and eval a YAMLScript string and return the result
func Load(input string) (data any, err error) {
	// Lock the OS thread for GraalVM native image calls
	runtime.LockOSThread()
	defer runtime.UnlockOSThread()

	// Attach a thread for this call
	var thread *C.graal_isolatethread_t
	rc := C.graal_attach_thread(isolate, &thread)
	if rc != 0 {
		return nil, errors.New("Failed to attach thread")
	}
	defer C.graal_detach_thread(thread)

	cs := C.CString(input)

	// Call 'load_ys_to_json' function in libys shared library:
	// Use the newly attached thread
	data_json := C.GoString(C.load_ys_to_json((C.longlong)(uintptr(unsafe.Pointer(thread))), cs))
	C.free(unsafe.Pointer(cs))

	// Decode the JSON response:
	var resp map[string]any
	err = json.Unmarshal([]byte(data_json), &resp)
	if err != nil {
		return
	}

	// Check for libys error in JSON response:
	if error_json, ok := resp["error"]; ok {
		err = errors.New(error_json.(map[string]any)["cause"].(string))
		return
	}

	// Get the response object from evaluating the YAMLScript string:
	var ok bool
	if data, ok = resp["data"]; !ok {
		err = errors.New("unexpected response from 'libys'")
	}

	return
}
