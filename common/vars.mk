COMMON := $(ROOT)/common

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

IS_ROOT :=
ifeq (0,$(shell id -u))
IS_ROOT := true
endif

LIBRARY_PATH := $(ROOT)/libyamlscript/lib
export $(DY)LD_LIBRARY_PATH := $(LIBRARY_PATH)
LIBYAMLSCRIPT_SO_PATH := $(LIBRARY_PATH)/libyamlscript.$(SO)
LIBYAMLSCRIPT_SO_NAME := $(LIBRARY_PATH)/libyamlscript

prefix ?= /usr/local
