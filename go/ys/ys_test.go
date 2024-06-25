package ys_test

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/xanni/yamlscript/go/ys"
)

func TestYamlScript(t *testing.T) {
	_, err := ys.Load(":")
	assert.Error(t, err)

	data, err := ys.Load("!yamlscript/v0/data\ntest:: inc(41)")
	assert.NoError(t, err)
	assert.Equal(t, map[string]any{"test": (float64)(42)}, data)
}
