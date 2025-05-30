export LANG := en_US.UTF-8

GIT-DIR := $(shell git rev-parse --git-common-dir 2>/dev/null)

ifndef YS_TMP
ifneq (,$(GIT-DIR))
  YS_TMP := $(shell cd -P $$(dirname $(GIT-DIR)) && pwd -P)/.git/tmp
else
  YS_TMP := $(ROOT)/.tmp
endif
endif

export TMPDIR := $(YS_TMP)/tmp
export TEMP := $(TMPDIR)
export TMP := $(TMPDIR)

ifeq (,$(wildcard $(TMPDIR)))
  $(shell mkdir -p $(TMPDIR))
endif

BUILD_BIN := $(YS_TMP)/bin

BUILD_BIN_YS_VERSION := 0.1.93

BUILD_BIN_YS := $(BUILD_BIN)/ys-$(BUILD_BIN_YS_VERSION)

YS_REPO_URL := https://github.com/yaml/yamlscript
YS_GH_API_URL := https://api.github.com/repos/yaml/yamlscript

YS_INSTALL_URL := https://yamlscript.org/install

COMMON := $(ROOT)/common

unexport YS_FORMATTER

export PATH := $(ROOT)/util:$(ROOT)/ys/bin:$(BUILD_BIN):$(PATH)

export YAMLSCRIPT_ROOT ?= $(ROOT)

export API_VERSION := 0
export YS_VERSION := $(shell grep '^version:' $(ROOT)/Meta | cut -d' ' -f2)
YAMLSCRIPT_VERSION := $(YS_VERSION)

ifdef v
  export TEST_VERBOSE := 1
endif

ostype := $(shell /bin/bash -c 'echo $$OSTYPE')
machtype := $(shell /bin/bash -c 'echo $$MACHTYPE')

ifneq (,$(findstring x86_64,$(machtype)))
  IS_INTEL := true
else ifneq (,$(findstring arm64,$(machtype)))
  IS_ARM := true
else ifneq (,$(findstring aarch64,$(machtype)))
  IS_ARM := true
endif

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
# Fix https://github.com/yaml/yamlscript/issues/210
LDCONFIG := $(shell PATH=/usr/sbin:$$PATH command -v ldconfig)
ifeq (,$(LDCONFIG))
$(error Can't find ldconfig)
endif
ifneq (,$(shell $(LDCONFIG) -p | grep $$'^\tlibz.so'))
  LIBZ := true
endif
endif

CURL := $(shell command -v curl)
ifdef CURL
ifdef YS_QUIET
  CURL := $(CURL) -sSL
else
  CURL := $(CURL) -SL
endif
endif

define need-curl
	@[[ "$(CURL)" ]] || { \
	  echo "*** 'curl' is required but not installed"; \
	  exit 1; \
	}
endef

TIME := time -p

ifeq (true,$(IS_ROOT))
  PREFIX ?= /usr/local
else
  PREFIX ?= $(HOME)/.local
endif


#------------------------------------------------------------------------------
# Set machine specific variables:
#------------------------------------------------------------------------------
ifeq (true,$(IS_LINUX))
  GRAALVM_SUBDIR :=

  ifeq (true,$(IS_INTEL))
    GRAALVM_ARCH := linux-x64

  else ifeq (true,$(IS_ARM))
    GRAALVM_ARCH := linux-aarch64

  else
    $(error Unsupported Linux MACHTYPE: $(machtype))
  endif

else ifeq (true,$(IS_MACOS))
  GRAALVM_SUBDIR := /Contents/Home

  ifeq (true,$(IS_ARM))
    GRAALVM_ARCH := macos-aarch64

  else ifeq (true,$(IS_INTEL))
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
GRAALVM_VER ?= 24
GRAALVM_TAR := graalvm-jdk-$(GRAALVM_VER)_$(GRAALVM_ARCH)_bin.tar.gz
GRAALVM_URL := $(GRAALVM_SRC)/$(GRAALVM_VER)/latest/$(GRAALVM_TAR)
GRAALVM_PATH ?= $(YS_TMP)/graalvm-oracle-$(GRAALVM_VER)

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
GRAALVM_PATH ?= $(YS_TMP)/graalvm-ce-$(GRAALVM_VER)
endif

GRAALVM_HOME := $(GRAALVM_PATH)$(GRAALVM_SUBDIR)
GRAALVM_DOWNLOAD := $(YS_TMP)/$(GRAALVM_TAR)
GRAALVM_INSTALLED := $(GRAALVM_HOME)/release

GRAALVM_O ?= 1
# qbm is Quick Build Mode
ifdef qbm
  GRAALVM_O := b
endif


#------------------------------------------------------------------------------
# Set MAVEN variables:
#------------------------------------------------------------------------------

MAVEN_VER := 3.9.9
MAVEN_SRC := https://dlcdn.apache.org/maven/maven-3/$(MAVEN_VER)/binaries
MAVEN_DIR := apache-maven-$(MAVEN_VER)
MAVEN_TAR := $(MAVEN_DIR)-bin.tar.gz
MAVEN_DIR := $(YS_TMP)/$(MAVEN_DIR)
MAVEN_URL := $(MAVEN_SRC)/$(MAVEN_TAR)
MAVEN_DOWNLOAD := $(YS_TMP)/$(MAVEN_TAR)
MAVEN_BIN := $(MAVEN_DIR)/bin
MAVEN_INSTALLED := $(MAVEN_BIN)/mvn
MAVEN_REPOSITORY := $(YS_TMP)/.m2/repository

export MAVEN_OPTS := -Duser.home=$(YS_TMP)

export LEIN_HOME := $(YS_TMP)/lein
export LEIN_JVM_OPTS := \
  -XX:+TieredCompilation \
  -XX:TieredStopAtLevel=1 \
  $(MAVEN_OPTS)

JAVA_INSTALLED := $(GRAALVM_INSTALLED) $(MAVEN_INSTALLED)

export PATH := $(MAVEN_BIN):$(PATH)


#------------------------------------------------------------------------------
# Set release asset variables:
#------------------------------------------------------------------------------

RELEASE_YS_NAME := ys-$(YS_VERSION)-$(GRAALVM_ARCH)
RELEASE_YS_TAR := $(RELEASE_YS_NAME).tar.xz

RELEASE_LYS_NAME := libyamlscript-$(YS_VERSION)-$(GRAALVM_ARCH)
RELEASE_LYS_TAR := $(RELEASE_LYS_NAME).tar.xz


#------------------------------------------------------------------------------
default::

build-bin-ys: $(BUILD_BIN_YS)

$(BUILD_BIN_YS):
	$(call need-curl)
	$(CURL) $(YS_INSTALL_URL) | \
	  PREFIX=$$(dirname $(BUILD_BIN)) \
	  VERSION=$(BUILD_BIN_YS_VERSION) \
	  BIN=1 bash

$(BUILD_BIN):
	mkdir -p $@
