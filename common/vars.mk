YS-VERSION := 0.1.97

include $(MAKES)/graalvm.mk
include $(MAKES)/maven.mk
include $(MAKES)/ys.mk

export TMPDIR := $(LOCAL-TMP)
export TEMP := $(TMPDIR)
export TMP := $(TMPDIR)

YS-REPO-URL := https://github.com/yaml/yamlscript
YS-GH-API-URL := https://api.github.com/repos/yaml/yamlscript

YS-INSTALL-URL := https://yamlscript.org/install

COMMON := $(ROOT)/common

unexport YS_FORMATTER

override PATH := $(ROOT)/util:$(ROOT)/ys/bin:$(PATH)
export PATH

export YAMLSCRIPT_ROOT ?= $(ROOT)

export API_VERSION := 0
export YS_VERSION := $(shell grep '^version:' $(ROOT)/Meta | cut -d' ' -f2)
YAMLSCRIPT_VERSION := $(YS_VERSION)

ifdef v
  export TEST_VERBOSE := 1
endif

ifdef IS-LINUX
  GCC := gcc -std=gnu99 -fPIC -shared
  SO := so
  DY :=
else ifdef IS-MACOS
  GCC := gcc -dynamiclib
  SO := dylib
  DY := DY
else
  $(error Unsupported OSTYPE: $(OS-TYPE))
endif

LIBZ := false
ifdef IS-MACOS
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
# Set JAVA and GRAALVM variables:
#------------------------------------------------------------------------------

JAVA-INSTALLED := $(GRAALVM) $(MAVEN)

GRAALVM-O ?= 1
# qbm is Quick Build Mode
ifdef qbm
  GRAALVM-O := b
endif


#------------------------------------------------------------------------------
# Set LEIN variables:
#------------------------------------------------------------------------------

export LEIN_HOME := $(LOCAL-HOME)/lein
export LEIN_JVM_OPTS := \
  -XX:+TieredCompilation \
  -XX:TieredStopAtLevel=1 \
  $(MAVEN-OPTS)


#------------------------------------------------------------------------------
# Set release asset variables:
#------------------------------------------------------------------------------

OA-linux-arm64 := linux-aarch64
OA-linux-int64 := linux-x64
OA-macos-arm64 := macos-aarch64
OA-macos-int64 := macos-x64

RELEASE-YS-NAME := ys-$(YS_VERSION)-$(OA-$(OS-ARCH))
RELEASE-YS-TAR := $(RELEASE-YS-NAME).tar.xz

RELEASE-LYS-NAME := libys-$(YS_VERSION)-$(OA-$(OS-ARCH))
RELEASE-LYS-TAR := $(RELEASE-LYS-NAME).tar.xz


#------------------------------------------------------------------------------
default::
