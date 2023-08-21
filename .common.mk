ifneq (GNU,$(firstword $(shell $(MAKE) --version)))
  $(error Error: $(MAKE) must be GNU)
endif

SHELL := bash
ROOT := $(shell cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)
export PATH := $(ROOT)/bin:$(PATH)

ifdef v
  export TEST_VERBOSE := 1
endif

#------------------------------------------------------------------------------
# Set machine specific variables:
#------------------------------------------------------------------------------
ostype := $(shell /bin/bash -c 'echo $$OSTYPE')
machtype := $(shell /bin/bash -c 'echo $$MACHTYPE')

ifneq (,$(findstring linux,$(ostype)))
  GCC := gcc -std=gnu99 -fPIC -shared
  SO := so
  DY :=
  GRAALVM_SUBDIR :=

  ifeq (x86_64-pc-linux-gnu,$(machtype))
    GRAALVM_ARCH := linux-x64

  else
    $(error Unsupported Linux MACHTYPE: $machtype)
  endif

else ifneq (,$(findstring darwin,$(ostype)))
  GCC := gcc -dynamiclib
  SO := dylib
  DY := DY
  GRAALVM_SUBDIR := /Contents/Home

  ifneq (,$(findstring arm64-apple-darwin,$(machtype)))
    GRAALVM_ARCH := macos-aarch64

  else ifneq (,$(findstring x86_64-apple-darwin,$(machtype)))
    GRAALVM_ARCH := macos-x64

  else
    $(error Unsupported MacOS MACHTYPE: $machtype)
  endif

else
  $(error Unsupported OSTYPE: $ostype)
endif

#------------------------------------------------------------------------------
# Set shared variables:
#------------------------------------------------------------------------------
LIBRARY_PATH := $(ROOT)/libyamlscript/lib
LIBYAMLSCRIPT_SO_PATH := $(LIBRARY_PATH)/libyamlscript.$(SO)
LIBYAMLSCRIPT_SO_NAME := $(LIBRARY_PATH)/libyamlscript
export $(DY)LD_LIBRARY_PATH := $(LIBRARY_PATH)

LEIN_COMMANDS := \
    check \
    classpath \
    compile \
    deps \
    deploy \
    install \
    jar \
    run \

#------------------------------------------------------------------------------
.SECONDEXPANSION:

.DELETE_ON_ERROR:

.PHONY: test
#------------------------------------------------------------------------------
default::

build::

clean::

distclean:: clean

clean-all::
	$(MAKE) -C $(ROOT) $@

repl:: repl-deps
repl-deps::

$(LEIN_COMMANDS)::
	lein $@
