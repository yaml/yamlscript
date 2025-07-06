$(if $(findstring GNU,$(firstword $(shell $(MAKE) --version))),,\
$(error Error: 'make' must be 'GNU make'))

ROOT := $(shell \
  cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))/..' && pwd -P)
export ROOT

ifdef YS_MAKES_LOCAL
  $(if $(wildcard $(YS_MAKES_LOCAL)/init.mk),,\
       $(error YS_MAKES_LOCAL=$(YS_MAKES_LOCAL) is not correct))
  include $(YS_MAKES_LOCAL)/init.mk
  MAKES-LOCAL-DIR := $(ROOT)/.local

else
  M := $(ROOT)/.cache/makes
  $(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)
  include $M/init.mk
  MAKES-LOCAL-DIR := $(ROOT)/.cache/.local
endif

MAKES-NO-RULES := true

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
