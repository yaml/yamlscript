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
