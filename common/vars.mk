BUILD_BIN := /tmp/yamlscript/bbin

COMMON := $(ROOT)/common

export PATH := $(ROOT)/bin:$(BUILD_BIN):$(PATH)

export YAMLSCRIPT_ROOT ?= $(ROOT)

ifdef v
  export TEST_VERBOSE := 1
endif

ostype := $(shell /bin/bash -c 'echo $$OSTYPE')
machtype := $(shell /bin/bash -c 'echo $$MACHTYPE')

ifneq (,$(findstring linux,$(ostype)))
  IS_LINUX := true
  GCC := gcc -std=gnu99 -fPIC -shared
  SO := so
  DY :=
else ifneq (,$(findstring darwin,$(ostype)))
  IS_MACOS := true
  GCC := gcc -dynamiclib
  SO := dylib
  DY := DY
else
  $(error Unsupported OSTYPE: $(ostype))
endif

IS_ROOT ?= undefined
ifneq (false,$(IS_ROOT))
ifeq (0,$(shell id -u))
  IS_ROOT := true
endif
endif

LIBZ := false
ifeq (true,$(IS_MACOS))
  LIBZ := true
else
ifneq (,$(shell ldconfig -p | grep $$'^\tlibz.$(SO) '))
  LIBZ := true
endif
endif

CURL := $(shell command -v curl)

LIBRARY_PATH := $(ROOT)/libyamlscript/lib
export $(DY)LD_LIBRARY_PATH := $(LIBRARY_PATH)
LIBYAMLSCRIPT_SO_PATH := $(LIBRARY_PATH)/libyamlscript.$(SO)
LIBYAMLSCRIPT_SO_NAME := $(LIBRARY_PATH)/libyamlscript

prefix ?= /usr/local
