include ../common/base.mk
include $(COMMON)/binding.mk

# Define crystal command
ifneq (,$(shell command -v crystal))
CRYSTAL ?= crystal
endif

BASE := $(ROOT)/crystal

VERSION := $(shell grep 'VERSION =' src/yamlscript/version.cr | cut -d'"' -f2)
YAMLSCRIPT_VERSION ?= 0.1.95
LIBYS_SO_PATH := $(ROOT)/libyamlscript/lib/$(LIBYS_SO_FQNP)
BUILD_DEPS := $(LIBYS_SO_PATH)

#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef CRYSTAL
test:: run-example test-crystal test-ffi
endif

run-example: $(BUILD_DEPS)
	$(CRYSTAL) run examples/simple.cr

test-crystal: $(BUILD_DEPS)
	$(CRYSTAL) spec

test-ffi: $(BUILD_DEPS)
	$(CRYSTAL) run test/ffi.cr

clean::
	$(RM) -r lib bin .crystal .shards shard.lock libyamlscript.so

$(LIBYS_SO_PATH):
	$(MAKE) -C $(ROOT)/libyamlscript $(LIBYS_SO_FQNP)

.PHONY: test
