package main

/*
#include <stdlib.h>
#include <stdint.h>
void *ys_alloc(void);
*/
import "C"

import (
	"encoding/json"
	"fmt"
	"sync"
	"unsafe"

	"github.com/glojurelang/glojure/pkg/glj"
)

type attachment struct {
	isolate unsafe.Pointer
	owner   C.uintptr_t
}

// Handles are C allocations. No Go pointer is passed to a binding.
var abiMutex sync.Mutex
var isolates = map[unsafe.Pointer]bool{}
var attachments = map[unsafe.Pointer]attachment{}
var allocateHandle = func() unsafe.Pointer { return C.ys_alloc() }

func attach(isolate unsafe.Pointer, owner C.uintptr_t) unsafe.Pointer {
	for handle, record := range attachments {
		if record.isolate == isolate && record.owner == owner {
			return handle
		}
	}
	handle := allocateHandle()
	if handle != nil {
		attachments[handle] = attachment{isolate, owner}
	}
	return handle
}

//export ys_create
func ys_create(isolateOut, threadOut *unsafe.Pointer, owner C.uintptr_t) C.int {
	abiMutex.Lock()
	defer abiMutex.Unlock()
	if isolateOut == nil && threadOut == nil {
		return 1
	}
	if isolateOut != nil {
		*isolateOut = nil
	}
	if threadOut != nil {
		*threadOut = nil
	}
	isolate := allocateHandle()
	if isolate == nil {
		return 1
	}
	thread := attach(isolate, owner)
	if thread == nil {
		C.free(isolate)
		return 1
	}
	isolates[isolate] = true
	if isolateOut != nil {
		*isolateOut = isolate
	}
	if threadOut != nil {
		*threadOut = thread
	}
	return 0
}

//export ys_attach
func ys_attach(isolate unsafe.Pointer, out *unsafe.Pointer, owner C.uintptr_t) C.int {
	abiMutex.Lock()
	defer abiMutex.Unlock()
	if out == nil {
		return 1
	}
	*out = nil
	if !isolates[isolate] {
		return 1
	}
	*out = attach(isolate, owner)
	if *out == nil {
		return 1
	}
	return 0
}

//export ys_detach
func ys_detach(thread unsafe.Pointer, owner C.uintptr_t) C.int {
	abiMutex.Lock()
	defer abiMutex.Unlock()
	record, ok := attachments[thread]
	if !ok || record.owner != owner {
		return 1
	}
	delete(attachments, thread)
	C.free(thread)
	return 0
}

//export ys_teardown
func ys_teardown(thread unsafe.Pointer, owner C.uintptr_t) C.int {
	abiMutex.Lock()
	defer abiMutex.Unlock()
	record, ok := attachments[thread]
	if !ok || record.owner != owner {
		return 1
	}
	for handle, other := range attachments {
		if other.isolate == record.isolate {
			delete(attachments, handle)
			C.free(handle)
		}
	}
	delete(isolates, record.isolate)
	C.free(record.isolate)
	return 0
}

//export ys_current
func ys_current(isolate unsafe.Pointer, owner C.uintptr_t) unsafe.Pointer {
	abiMutex.Lock()
	defer abiMutex.Unlock()
	for handle, record := range attachments {
		if record.isolate == isolate && record.owner == owner {
			return handle
		}
	}
	return nil
}

//export ys_isolate
func ys_isolate(thread unsafe.Pointer) unsafe.Pointer {
	abiMutex.Lock()
	defer abiMutex.Unlock()
	return attachments[thread].isolate
}

type evaluation struct {
	source string
	result chan string
}

var evaluations = make(chan evaluation)
var evaluatorOnce sync.Once

func evaluate(source string) (result string) {
	defer func() {
		if failure := recover(); failure != nil {
			encoded, _ := json.Marshal(map[string]any{"error": map[string]any{
				"cause": fmt.Sprint(failure), "type": "native",
			}})
			result = string(encoded)
		}
	}()
	return glj.Var("libys", "load-ys-to-json").Invoke(nil, source).(string)
}

//export ys_evaluate
func ys_evaluate(thread unsafe.Pointer, source *C.char) *C.char {
	// Hold the registry lock through evaluation so teardown cannot race it.
	abiMutex.Lock()
	defer abiMutex.Unlock()
	if _, ok := attachments[thread]; !ok || source == nil {
		return C.CString(`{"error":{"cause":"Invalid libys handle or source","type":"native"}}`)
	}
	// Glojure dynamic bindings belong to a goroutine. Keep all evaluations on
	// one worker so runtime initialization and subsequent calls share them.
	evaluatorOnce.Do(func() {
		go func() {
			for request := range evaluations {
				request.result <- evaluate(request.source)
			}
		}()
	})
	request := evaluation{C.GoString(source), make(chan string)}
	evaluations <- request
	return C.CString(<-request.result)
}
