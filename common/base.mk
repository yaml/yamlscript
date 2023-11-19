ifneq (GNU,$(firstword $(shell $(MAKE) --version)))
  $(error Error: $(MAKE) must be GNU)
endif

SHELL := bash

ROOT := $(shell \
  cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))/..' && pwd -P)

include $(ROOT)/common/vars.mk

SUBDIR = $(shell pwd)
SUBDIR := $(SUBDIR:$(ROOT)/%=%)

#------------------------------------------------------------------------------
.SECONDEXPANSION:

.DELETE_ON_ERROR:

.PHONY: test

#------------------------------------------------------------------------------
default::

build::

clean::

realclean:: clean

distclean:: realclean

chown::
	$(MAKE) -C $(ROOT) $@

clean-all::
	$(MAKE) -C $(ROOT) $@

docker-build::

docker-test::

always:
