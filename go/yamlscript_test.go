package yamlscript_test

import (
	"testing"

	"github.com/yaml/yamlscript/go"
)

func TestYamlScript(t *testing.T) {
	_, err := yamlscript.Load(":")
	if err == nil {
		t.Fatal("expected error for invalid YAMLScript")
	}

	data, err := yamlscript.Load("!ys-0:\ntest:: inc(41)")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	got, ok := data.(map[string]any)
	if !ok {
		t.Fatalf("expected map[string]any, got %T", data)
	}
	if got["test"] != float64(42) {
		t.Fatalf("expected test value 42, got %v", got["test"])
	}
}
