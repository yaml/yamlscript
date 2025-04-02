include ../common/base.mk
include $(COMMON)/binding.mk

# Define crystal command
CRYSTAL ?= crystal

ifdef DOCKERENV
	CRYSTAL_BIN := $(YS_TMP)/crystal/bin
else
	CRYSTAL_BIN := $(ROOT)/crystal/bin
endif

VERSION := $(shell grep 'VERSION =' src/yamlscript/version.cr | cut -d'"' -f2)
YAMLSCRIPT_VERSION ?= 0.1.95
YAMLSCRIPT_FNP ?= $(HOME)/.local/lib/libyamlscript.so.$(YAMLSCRIPT_VERSION)
LIBYAMLSCRIPT_SO_FQNP ?= $(YAMLSCRIPT_FNP)

#------------------------------------------------------------------------------

version:
	@echo $(VERSION)

# Build the library and documentation
build:: build-doc build-lib
	@echo "Crystal binding built successfully"

# Copy the library file to the local lib directory
build-lib: $(LIBYAMLSCRIPT_SO_FQNP)
	@mkdir -p lib
	@echo "Copying $(LIBYAMLSCRIPT_SO_FQNP) to lib/"
	@cp -fL $(LIBYAMLSCRIPT_SO_FQNP) lib/
	@ln -sf libyamlscript.so.$(YAMLSCRIPT_VERSION) lib/libyamlscript.so

.PHONY: test run-example
ifdef CRYSTAL
test:: test-wrapper
endif

run-example: build-lib
	@CRYSTAL_LIBRARY_PATH="$(PWD)/lib" LD_LIBRARY_PATH="$(PWD)/lib:$(LD_LIBRARY_PATH)" $(CRYSTAL) run examples/simple.cr

# Use wrapper script for tests
test-wrapper:
	@./test_wrapper.sh

test-crystal: build-lib
	@CRYSTAL_LIBRARY_PATH="$(PWD)/lib" LD_LIBRARY_PATH="$(PWD)/lib:$(LD_LIBRARY_PATH)" $(CRYSTAL) spec

test-ffi: build-lib
	@echo "Running Crystal binding FFI test..."
	@CRYSTAL_LIBRARY_PATH="$(PWD)/lib" LD_LIBRARY_PATH="$(PWD)/lib:$(LD_LIBRARY_PATH)" $(CRYSTAL) run test/ffi.cr

clean::
	$(RM) -r lib bin .crystal .shards shard.lock libyamlscript.so
	@echo

realclean::
	@echo 