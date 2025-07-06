include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/crystal.mk
include $(MAKES)/shell.mk

BUILD-DEPS := $(CRYSTAL)

# Set library path for Crystal compiler
CRYSTAL-ENV := \
  LIBRARY_PATH=$(ROOT)/libys/lib \
  LD_LIBRARY_PATH=$(ROOT)/libys/lib

#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

test:: test-example test-crystal test-ffi

test-example: $(BUILD-DEPS)
	$(CRYSTAL-ENV) crystal run examples/simple.cr

test-crystal: $(BUILD-DEPS)
	$(CRYSTAL-ENV) crystal spec

test-ffi: $(BUILD-DEPS)
	$(CRYSTAL-ENV) crystal run test/ffi.cr

release:
	$(ROOT)/util/release-crystal

clean::
	$(RM) -r lib bin .crystal .shards shard.lock libys.so
