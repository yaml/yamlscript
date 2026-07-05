include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/crystal.mk
include $(MAKES)/shell.mk

BUILD-DEPS := $(CRYSTAL)

# Set library path for Crystal compiler
ifeq ($(OS-NAME),windows)
# Native tools need Windows-style paths and a ';' separator. The
# mingw linker finds libys.dll via LIBRARY_PATH; at run time the OS
# loader finds it via PATH:
CRYSTAL-ENV := \
  LIBRARY_PATH="$$(cygpath -m $(ROOT)/libys/lib);$$(cygpath -m \
    $(CRYSTAL_MINGW_LIB) 2>/dev/null)"
else
CRYSTAL-ENV := \
  LIBRARY_PATH=$(ROOT)/libys/lib:$(CRYSTAL_MINGW_LIB) \
  LD_LIBRARY_PATH=$(ROOT)/libys/lib:$(CRYSTAL_MINGW_LIB)
endif

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
