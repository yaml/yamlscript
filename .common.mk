ifneq (GNU,$(firstword $(shell $(MAKE) --version)))
  $(error Error: $(MAKE) must be GNU)
endif

SHELL := bash

ROOT := $(shell cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

export PATH := $(ROOT)/bin:$(PATH)

export YAMLSCRIPT_ROOT ?= $(ROOT)

ifdef v
  export TEST_VERBOSE := 1
endif

ostype := $(shell /bin/bash -c 'echo $$OSTYPE')
machtype := $(shell /bin/bash -c 'echo $$MACHTYPE')

ifneq (,$(findstring linux,$(ostype)))
  GCC := gcc -std=gnu99 -fPIC -shared
  SO := so
  DY :=
else ifneq (,$(findstring darwin,$(ostype)))
  GCC := gcc -dynamiclib
  SO := dylib
  DY := DY
else
  $(error Unsupported OSTYPE: $(ostype))
endif

LIBRARY_PATH := $(ROOT)/libyamlscript/lib
export $(DY)LD_LIBRARY_PATH := $(LIBRARY_PATH)
LIBYAMLSCRIPT_SO_PATH := $(LIBRARY_PATH)/libyamlscript.$(SO)
LIBYAMLSCRIPT_SO_NAME := $(LIBRARY_PATH)/libyamlscript

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
