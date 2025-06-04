include ../common/base.mk
include $(COMMON)/binding.mk

# Define crystal command
ifneq (,$(shell command -v crystal))
CRYSTAL ?= crystal
endif

BASE := $(ROOT)/crystal

LIBYS-SO-PATH := $(ROOT)/libyamlscript/lib/$(LIBYS-SO-FQNP)
BUILD-DEPS := $(LIBYS-SO-PATH)

#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef CRYSTAL
test:: test-example test-crystal test-ffi
endif

test-example: $(BUILD-DEPS)
	$(CRYSTAL) run examples/simple.cr

test-crystal: $(BUILD-DEPS)
	$(CRYSTAL) spec

test-ffi: $(BUILD-DEPS)
	$(CRYSTAL) run test/ffi.cr

release:
	$(ROOT)/util/release-crystal

clean::
	$(RM) -r lib bin .crystal .shards shard.lock libyamlscript.so

$(LIBYS-SO-PATH):
	$(MAKE) -C $(ROOT)/libyamlscript $(LIBYS-SO-FQNP)
