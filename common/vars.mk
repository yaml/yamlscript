export LANG := en_US.UTF-8

GIT-DIR := $(shell git rev-parse --git-common-dir)
ifeq (,$(GIT-DIR))
  $(error Can't determine .git directory location)
endif
GIT-DIR := $(shell cd $(GIT-DIR) && pwd -P)

LOCAL-ROOT := $(GIT-DIR)/.local
LOCAL-BIN := $(LOCAL-ROOT)/bin
LOCAL-CACHE := $(LOCAL-ROOT)/cache
LOCAL-HOME := $(LOCAL-ROOT)/home
LOCAL-TMP := $(LOCAL-ROOT)/tmp
_ := $(shell mkdir -p $(LOCAL-BIN) $(LOCAL-CACHE) $(LOCAL-HOME) $(LOCAL-TMP))

export TMPDIR := $(LOCAL-TMP)
export TEMP := $(TMPDIR)
export TMP := $(TMPDIR)

BUILD-BIN := $(LOCAL-BIN)

BUILD-BIN-YS-VERSION := 0.1.93

BUILD-BIN-YS := $(BUILD-BIN)/ys-$(BUILD-BIN-YS-VERSION)

YS-REPO-URL := https://github.com/yaml/yamlscript
YS-GH-API-URL := https://api.github.com/repos/yaml/yamlscript

YS-INSTALL-URL := https://yamlscript.org/install

COMMON := $(ROOT)/common

unexport YS_FORMATTER

export PATH := $(ROOT)/util:$(ROOT)/ys/bin:$(BUILD-BIN):$(PATH)

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
  IS-INTEL := true
else ifneq (,$(findstring arm64,$(machtype)))
  IS-ARM := true
else ifneq (,$(findstring aarch64,$(machtype)))
  IS-ARM := true
endif

ifneq (,$(findstring linux,$(ostype)))
  IS-LINUX := true
  GCC := gcc -std=gnu99 -fPIC -shared
  SO := so
  DY :=
else ifneq (,$(findstring darwin,$(ostype)))
  IS-MACOS := true
  GCC := gcc -dynamiclib
  SO := dylib
  DY := DY
else
  $(error Unsupported OSTYPE: $(ostype))
endif

IS-ROOT ?= undefined
ifneq (false,$(IS-ROOT))
ifeq (0,$(shell id -u))
  IS-ROOT := true
endif
endif

LIBZ := false
ifeq (true,$(IS-MACOS))
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

ifeq (true,$(IS-ROOT))
  PREFIX ?= /usr/local
else
  PREFIX ?= $(HOME)/.local
endif


#------------------------------------------------------------------------------
# Set machine specific variables:
#------------------------------------------------------------------------------
ifeq (true,$(IS-LINUX))
  GRAALVM-SUBDIR :=

  ifeq (true,$(IS-INTEL))
    GRAALVM-ARCH := linux-x64

  else ifeq (true,$(IS-ARM))
    GRAALVM-ARCH := linux-aarch64

  else
    $(error Unsupported Linux MACHTYPE: $(machtype))
  endif

else ifeq (true,$(IS-MACOS))
  GRAALVM-SUBDIR := /Contents/Home

  ifeq (true,$(IS-ARM))
    GRAALVM-ARCH := macos-aarch64

  else ifeq (true,$(IS-INTEL))
    GRAALVM-ARCH := macos-x64

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
ifndef GRAALVM-CE
GRAALVM-SRC := https://download.oracle.com/graalvm
GRAALVM-VER ?= 24
GRAALVM-TAR := graalvm-jdk-$(GRAALVM-VER)_$(GRAALVM-ARCH)_bin.tar.gz
GRAALVM-URL := $(GRAALVM-SRC)/$(GRAALVM-VER)/latest/$(GRAALVM-TAR)
GRAALVM-PATH ?= $(LOCAL-CACHE)/graalvm-oracle-$(GRAALVM-VER)

### For GraalVM CE (Community Edition) ########################################
else
GRAALVM-SRC := https://github.com/graalvm/graalvm-ce-builds/releases/download
GRAALVM-VER ?= 21
  ifeq (21,$(GRAALVM-VER))
    override GRAALVM-VER := jdk-21.0.0
  endif
  ifeq (17,$(GRAALVM-VER))
    override GRAALVM-VER := jdk-17.0.8
  endif
GRAALVM-TAR := graalvm-community-$(GRAALVM-VER)_$(GRAALVM-ARCH)_bin.tar.gz
GRAALVM-URL := $(GRAALVM-SRC)/$(GRAALVM-VER)/$(GRAALVM-TAR)
GRAALVM-PATH ?= $(LOCAL-CACHE)/graalvm-ce-$(GRAALVM-VER)
endif

GRAALVM-HOME := $(GRAALVM-PATH)$(GRAALVM-SUBDIR)
GRAALVM-DOWNLOAD := $(LOCAL-CACHE)/$(GRAALVM-TAR)
GRAALVM-INSTALLED := $(GRAALVM-HOME)/release

GRAALVM-O ?= 1
# qbm is Quick Build Mode
ifdef qbm
  GRAALVM-O := b
endif


#------------------------------------------------------------------------------
# Set MAVEN variables:
#------------------------------------------------------------------------------

MAVEN-VER := 3.9.9
MAVEN-SRC := https://dlcdn.apache.org/maven/maven-3/$(MAVEN-VER)/binaries
MAVEN-DIR := apache-maven-$(MAVEN-VER)
MAVEN-TAR := $(MAVEN-DIR)-bin.tar.gz
MAVEN-DIR := $(LOCAL-CACHE)/$(MAVEN-DIR)
MAVEN-URL := $(MAVEN-SRC)/$(MAVEN-TAR)
MAVEN-DOWNLOAD := $(LOCAL-CACHE)/$(MAVEN-TAR)
MAVEN-BIN := $(MAVEN-DIR)/bin
MAVEN-INSTALLED := $(MAVEN-BIN)/mvn
MAVEN-REPOSITORY := $(LOCAL-HOME)/.m2/repository

MAVEN-OPTS := -Duser.home=$(LOCAL-HOME)
export MAVEN_OPTS := $(MAVEN-OPTS)

export LEIN_HOME := $(LOCAL-HOME)/lein
export LEIN_JVM_OPTS := \
  -XX:+TieredCompilation \
  -XX:TieredStopAtLevel=1 \
  $(MAVEN-OPTS)

JAVA-INSTALLED := $(GRAALVM-INSTALLED) $(MAVEN-INSTALLED)

export PATH := $(MAVEN-BIN):$(PATH)


#------------------------------------------------------------------------------
# Set release asset variables:
#------------------------------------------------------------------------------

RELEASE-YS-NAME := ys-$(YS_VERSION)-$(GRAALVM-ARCH)
RELEASE-YS-TAR := $(RELEASE-YS-NAME).tar.xz

RELEASE-LYS-NAME := libyamlscript-$(YS_VERSION)-$(GRAALVM-ARCH)
RELEASE-LYS-TAR := $(RELEASE-LYS-NAME).tar.xz


#------------------------------------------------------------------------------
default::

build-bin-ys: $(BUILD-BIN-YS)

$(BUILD-BIN-YS):
	$(call need-curl)
	$(CURL) $(YS-INSTALL-URL) | \
	  PREFIX=$$(dirname $(BUILD-BIN)) \
	  VERSION=$(BUILD-BIN-YS-VERSION) \
	  BIN=1 bash

$(BUILD-BIN):
	mkdir -p $@
