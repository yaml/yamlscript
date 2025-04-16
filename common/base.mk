ifneq (GNU,$(firstword $(shell $(MAKE) --version)))
  $(error Error: 'make' must be 'GNU make')
endif

SHELL := bash

ROOT := $(shell \
  cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))/..' && pwd -P)

export ROOT

include $(ROOT)/common/vars.mk

SUBDIR = $(shell pwd)
SUBDIR := $(SUBDIR:$(ROOT)/%=%)

export YSLANG := $(SUBDIR)


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

always:

env::
	@env | sort | less -FRX
