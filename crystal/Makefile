include ../common/base.mk
include $(COMMON)/binding.mk

# Define crystal command
ifneq (,$(shell command -v crystal))
CRYSTAL ?= crystal
endif

BASE := $(ROOT)/crystal

LIBYS_SO_PATH := $(ROOT)/libyamlscript/lib/$(LIBYS_SO_FQNP)
BUILD_DEPS := $(LIBYS_SO_PATH)

#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef CRYSTAL
test:: test-example test-crystal test-ffi
endif

test-example: $(BUILD_DEPS)
	$(CRYSTAL) run examples/simple.cr

test-crystal: $(BUILD_DEPS)
	$(CRYSTAL) spec

test-ffi: $(BUILD_DEPS)
	$(CRYSTAL) run test/ffi.cr

release:
	$(ROOT)/util/release-crystal

clean::
	$(RM) -r lib bin .crystal .shards shard.lock libyamlscript.so

$(LIBYS_SO_PATH):
	$(MAKE) -C $(ROOT)/libyamlscript $(LIBYS_SO_FQNP)
