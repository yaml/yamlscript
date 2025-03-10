package yamlscript_test

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/yaml/yamlscript/go"
)

func TestYamlScript(t *testing.T) {
	_, err := yamlscript.Load(":")
	assert.Error(t, err)

        data, err := yamlscript.Load("!YS-v0:\ntest:: inc(41)")
	assert.NoError(t, err)
	assert.Equal(t, map[string]any{"test": (float64)(42)}, data)
}
