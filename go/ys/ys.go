package ys

// #cgo CFLAGS: -I/usr/local/include
// #cgo LDFLAGS: -L/usr/local/lib -lyamlscript
// #include <libyamlscript.h>
// #include <stdlib.h>
import "C"
import (
	"encoding/json"
	"errors"
	"log"
	"runtime"
	"unsafe"
)

// Create a new GraalVM isolatethread for life of the YAMLScript instance:
var isolatethread unsafe.Pointer

// Tear down the isolate thread to free resources
func free() {
	rc := C.graal_tear_down_isolate((*C.graal_isolatethread_t)(isolatethread))
	if rc != 0 {
		log.Fatal("Failed to tear down isolate")
	}
}

// YAMLScript instance constructor
func init() {
	rc := C.graal_create_isolate(nil, nil, (**C.graal_isolatethread_t)(unsafe.Pointer(&isolatethread)))
	if rc != 0 {
		log.Fatal("Failed to create isolate")
	}
	runtime.SetFinalizer(&isolatethread, free)
}

// Compile and eval a YAMLScript string and return the result
func Load(input string) (data any, err error) {
	cs := C.CString(input)

	// Call 'load_ys_to_json' function in libyamlscript shared library:
	data_json := C.GoString(C.load_ys_to_json(C.longlong(uintptr(isolatethread)), cs))
	C.free(unsafe.Pointer(cs))

	// Decode the JSON response:
	var resp map[string]any
	err = json.Unmarshal([]byte(data_json), &resp)
	if err != nil {
		return
	}

	// Check for libyamlscript error in JSON response:
	if error_json, ok := resp["error"]; ok {
		err = errors.New(error_json.(map[string]any)["cause"].(string))
		return
	}

	// Get the response object from evaluating the YAMLScript string:
	var ok bool
	if data, ok = resp["data"]; !ok {
		err = errors.New("unexpected response from 'libyamlscript'")
	}

	return
}
