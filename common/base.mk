$(if $(findstring GNU,$(firstword $(shell $(MAKE) --version))),,\
$(error Error: 'make' must be 'GNU make'))

SHELL := bash

ROOT-MAKE := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/..)
ROOT := $(shell cd '$(ROOT-MAKE)' && pwd -P)
ifeq ($(OS),Windows_NT)
ROOT := $(shell cygpath -am '$(ROOT)')
endif
ROOT-MAKE := $(ROOT)
export ROOT

M := $(ROOT-MAKE)/.cache/makes
$(shell mkdir -p "$(ROOT)/.cache")
$(shell test -f "$M/init.mk" || { \
  rm -rf "$M" && git clone -q https://github.com/makeplus/makes "$M"; })
include $M/init.mk
MAKES-LOCAL-DIR := $(ROOT-MAKE)/.cache/.local

MAKES-NO-RULES := true

COMMON := $(ROOT-MAKE)/common
include $(COMMON)/vars.mk

SUBDIR = $(shell pwd)
ifeq ($(OS),Windows_NT)
SUBDIR := $(shell cygpath -am '$(CURDIR)')
endif
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
