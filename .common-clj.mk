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

ifdef w
  export WARN_ON_REFLECTION := 1
endif

LEIN_COMMANDS := \
    check \
    classpath \
    compile \
    deps \
    deploy \
    install \
    jar \
    run \

$(LEIN_COMMANDS)::
	lein $@

repl:: repl-deps
repl-deps::

deps-graph::
	lein deps :tree
