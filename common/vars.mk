BUILD_BIN := /tmp/yamlscript/bin

COMMON := $(ROOT)/common

export PATH := $(ROOT)/util:$(ROOT)/ys/bin:$(BUILD_BIN):$(PATH)

export YAMLSCRIPT_ROOT ?= $(ROOT)

export API_VERSION := 0
export YS_VERSION := $(shell grep '^version:' $(ROOT)/Meta | cut -d' ' -f2)

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

TIME := time -p

LIBRARY_PATH := $(ROOT)/libyamlscript/lib
export $(DY)LD_LIBRARY_PATH := $(LIBRARY_PATH)
LIBYAMLSCRIPT_SO_NAME := $(LIBRARY_PATH)/libyamlscript
LIBYAMLSCRIPT_SO_FQNP := $(LIBYAMLSCRIPT_SO_NAME).$(SO).$(YS_VERSION)
LIBYAMLSCRIPT_SO_BASE := $(LIBRARY_PATH)/libyamlscript.$(SO)
LIBYAMLSCRIPT_SO_APIP := $(LIBYAMLSCRIPT_SO_BASE).$(API_VERSION)

PREFIX ?= /usr/local

#------------------------------------------------------------------------------
# Set machine specific variables:
#------------------------------------------------------------------------------
ifneq (,$(findstring linux,$(ostype)))
  GRAALVM_SUBDIR :=

  ifeq (ok,$(shell [[ $(machtype) == x86_64-*-linux* ]] && echo ok))
    GRAALVM_ARCH := linux-x64

  else
    $(error Unsupported Linux MACHTYPE: $(machtype))
  endif

else ifeq (true,$(IS_MACOS))
  GRAALVM_SUBDIR := /Contents/Home

  ifneq (,$(findstring arm64-apple-darwin,$(machtype)))
    GRAALVM_ARCH := macos-aarch64

  else ifneq (,$(findstring x86_64-apple-darwin,$(machtype)))
    GRAALVM_ARCH := macos-x64

  else
    $(error Unsupported MacOS MACHTYPE: $(machtype))
  endif

else
  $(error Unsupported OSTYPE: $(ostype))
endif

#------------------------------------------------------------------------------
# Set GRAALVM variables:
#------------------------------------------------------------------------------

### For Orable GraalVM No-Fee #################################################
ifndef GRAALVM_CE
GRAALVM_SRC := https://download.oracle.com/graalvm
GRAALVM_VER ?= 21
GRAALVM_TAR := graalvm-jdk-$(GRAALVM_VER)_$(GRAALVM_ARCH)_bin.tar.gz
GRAALVM_URL := $(GRAALVM_SRC)/$(GRAALVM_VER)/latest/$(GRAALVM_TAR)
GRAALVM_PATH ?= /tmp/graalvm-oracle-$(GRAALVM_VER)

### For GraalVM CE (Community Edition) ########################################
else
GRAALVM_SRC := https://github.com/graalvm/graalvm-ce-builds/releases/download
GRAALVM_VER ?= 21
  ifeq (21,$(GRAALVM_VER))
    override GRAALVM_VER := jdk-21.0.0
  endif
  ifeq (17,$(GRAALVM_VER))
    override GRAALVM_VER := jdk-17.0.8
  endif
GRAALVM_TAR := graalvm-community-$(GRAALVM_VER)_$(GRAALVM_ARCH)_bin.tar.gz
GRAALVM_URL := $(GRAALVM_SRC)/$(GRAALVM_VER)/$(GRAALVM_TAR)
GRAALVM_PATH ?= /tmp/graalvm-ce-$(GRAALVM_VER)
endif

GRAALVM_HOME := $(GRAALVM_PATH)$(GRAALVM_SUBDIR)
GRAALVM_DOWNLOAD := /tmp/$(GRAALVM_TAR)
GRAALVM_INSTALLED := $(GRAALVM_HOME)/release

GRAALVM_O ?= 1
ifdef qbm
  GRAALVM_O := b
endif


#------------------------------------------------------------------------------
# Set release asset variables:
#------------------------------------------------------------------------------

RELEASE_YS_NAME := ys-$(YS_VERSION)-$(GRAALVM_ARCH)
RELEASE_YS_TAR := $(RELEASE_YS_NAME).tar.xz

RELEASE_LYS_NAME := libyamlscript-$(YS_VERSION)-$(GRAALVM_ARCH)
RELEASE_LYS_TAR := $(RELEASE_LYS_NAME).tar.xz


#------------------------------------------------------------------------------
# Perl ZILD
#------------------------------------------------------------------------------

ZILD := \
    cpan \
    cpanshell \
    dist \
    distdir \
    distshell \
    disttest \
    install \
    release \
    update
