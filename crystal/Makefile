include ../common/base.mk
include $(COMMON)/binding.mk

# Define crystal command
ifneq (,$(shell command -v crystal))
CRYSTAL ?= crystal
endif

BASE := $(ROOT)/crystal

LIBYS-SO-PATH := $(ROOT)/libys/lib/$(LIBYS-SO-FQNP)
BUILD-DEPS := $(LIBYS-SO-PATH)

# Set library path for Crystal compiler
CRYSTAL-ENV := \
  LIBRARY_PATH=$(ROOT)/libys/lib \
  LD_LIBRARY_PATH=$(ROOT)/libys/lib \

#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef CRYSTAL
test:: test-example test-crystal test-ffi
endif

test-example: $(BUILD-DEPS)
	$(CRYSTAL-ENV) $(CRYSTAL) run examples/simple.cr

test-crystal: $(BUILD-DEPS)
	$(CRYSTAL-ENV) $(CRYSTAL) spec

test-ffi: $(BUILD-DEPS)
	$(CRYSTAL-ENV) $(CRYSTAL) run test/ffi.cr

release:
	$(ROOT)/util/release-crystal

clean::
	$(RM) -r lib bin .crystal .shards shard.lock libys.so

$(LIBYS-SO-PATH):
	$(MAKE) -C $(ROOT)/libys $(LIBYS-SO-FQNP)
