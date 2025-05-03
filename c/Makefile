include ../common/base.mk
include $(COMMON)/binding.mk

CMAKE ?= cmake
SHARED ?= 1
BUILD_TYPE ?= Release

CMAKE_ARGS_CFG ?=
CMAKE_ARGS_BUILD ?= --parallel

CMAKE_ARGS_BUILD += --config $(BUILD_TYPE)

config::
	$(CMAKE) -S src -B src/build \
	    -DBUILD_SHARED_LIBS=$(SHARED) \
	    -DCMAKE_BUILD_TYPE=$(BUILD_TYPE) \
	    $(CMAKE_ARGS_CFG)
build:: config
	$(CMAKE) --build src/build $(CMAKE_ARGS_BUILD)
testwtf: build
	$(CMAKE) -S test -B test/build \
	    -DBUILD_SHARED_LIBS=$(SHARED) \
	    -DCMAKE_BUILD_TYPE=$(BUILD_TYPE)
	$(CMAKE) --build test/build $(CMAKE_ARGS_BUILD) --target all
	LD_LIBRARY_PATH=$(LD_LIBRARY_PATH) kdbg test/build/yamlscript_c_test
	$(CMAKE) --build test/build $(CMAKE_ARGS_BUILD) --target test-run
