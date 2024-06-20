package yamlscript_test

import (
	"testing"
	"yamlscript"

	"github.com/stretchr/testify/assert"
)

func TestYamlScript(t *testing.T) {
	_, err := yamlscript.Load(":")
	assert.Error(t, err)

	data, err := yamlscript.Load("!yamlscript/v0/data\ntest:: inc(41)")
	assert.NoError(t, err)
	assert.Equal(t, map[string]any{"test": (float64)(42)}, data)
}
