package main

import (
	"testing"
	"unsafe"
)

func TestAllocationFailure(t *testing.T) {
	original := allocateHandle
	defer func() { allocateHandle = original }()
	for failAt := 0; failAt < 2; failAt++ {
		calls := 0
		allocateHandle = func() unsafe.Pointer {
			calls++
			if calls == failAt+1 {
				return nil
			}
			return original()
		}
		var isolate, thread unsafe.Pointer
		if ys_create(&isolate, &thread, 1) == 0 {
			t.Fatal("allocation failure reported success")
		}
		if isolate != nil || thread != nil || len(isolates)+len(attachments) != 0 {
			t.Fatal("failed creation left outputs or registry entries")
		}
	}
}

func TestHandleCleanup(t *testing.T) {
	for i := 0; i < 100; i++ {
		var isolate, thread, other unsafe.Pointer
		if ys_create(&isolate, &thread, 1) != 0 || isolate == nil || thread == nil {
			t.Fatal("create failed")
		}
		if ys_attach(isolate, &other, 2) != 0 || other == thread {
			t.Fatal("second native thread did not get its own attachment")
		}
		if ys_detach(thread, 2) == 0 || ys_teardown(thread, 2) == 0 {
			t.Fatal("wrong native thread accepted")
		}
		if ys_teardown(thread, 1) != 0 || len(isolates)+len(attachments) != 0 {
			t.Fatal("teardown did not release every handle")
		}
	}
}
