include $(COMMON)/version.mk
YAMLSCRIPT_ENGINE ?= glojure
ifeq ($(YAMLSCRIPT_ENGINE),graalvm)
include $(MAKES)/graalvm.mk
else
include $(MAKES)/java.mk
endif
include $(MAKES)/maven.mk
ifeq ($(OS-NAME),windows)
YS ?= $(ROOT)/ys/bin/ys-$(YS_VERSION).exe
else
ifndef YS
include $(MAKES)/yamlscript.mk
endif
endif

export YS_TMPDIR := $(LOCAL-TMP)
export TMPDIR := $(LOCAL-TMP)
export TEMP := $(TMPDIR)
export TMP := $(TMPDIR)

YS-REPO-URL := https://github.com/yaml/yamlscript
YS-GH-API-URL := https://api.github.com/repos/yaml/yamlscript

YS-INSTALL-URL := https://in-1.cc

unexport YS_FORMATTER

override PATH := $(ROOT)/util:$(ROOT)/ys/bin:$(LOCAL-BIN):$(PATH)

export YAMLSCRIPT_ROOT ?= $(ROOT)

export API_VERSION := 0
export YS_VERSION := $(shell grep '^version:' $(ROOT)/Meta | cut -d' ' -f2)
YAMLSCRIPT_VERSION := $(YS_VERSION)

GLOAT-TARGET-OS := $(firstword $(subst /, ,$(GLOAT_PLATFORM)))
TARGET-OS := $(or $(GLOAT-TARGET-OS),$(OS-NAME))
HOST-GLOAT-OS := $(if $(filter macos,$(OS-NAME)),darwin,$(OS-NAME))
HOST-GLOAT-ARCH := $(if $(filter int64,$(ARCH-NAME)),amd64,$(ARCH-NAME))
HOST-GLOAT-PLATFORM := $(HOST-GLOAT-OS)/$(HOST-GLOAT-ARCH)
GLOAT-CROSS := $(strip $(if $(GLOAT_PLATFORM),\
  $(if $(filter $(HOST-GLOAT-PLATFORM),$(GLOAT_PLATFORM)),,1)))

ifdef v
  export TEST_VERBOSE := 1
endif

ifeq ($(TARGET-OS),linux)
  GCC := gcc -std=gnu99 -fPIC -shared
  SO := so
  DY :=
else ifeq ($(TARGET-OS),darwin)
  GCC := gcc -dynamiclib
  SO := dylib
  DY := DY
else ifeq ($(TARGET-OS),macos)
  GCC := gcc -dynamiclib
  SO := dylib
  DY := DY
else ifeq ($(TARGET-OS),windows)
  SO := dll
  DY :=
else ifeq ($(TARGET-OS),freebsd)
  GCC := cc -std=gnu99 -fPIC -shared
  SO := so
  DY :=
else ifeq ($(TARGET-OS),wasip1)
  SO := wasm
  DY :=
else
  $(error Unsupported target OS: $(TARGET-OS))
endif

LIBZ := false
ifeq ($(OS-NAME),macos)
  LIBZ := true
else ifeq ($(OS-NAME),windows)
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

ifeq ($(OS-NAME),windows)
TIME :=
else
TIME := time -p
endif

ifeq (true,$(IS-ROOT))
  PREFIX ?= /usr/local
else
  PREFIX ?= $(HOME)/.local
endif


#------------------------------------------------------------------------------
# Set JAVA and GRAALVM variables:
#------------------------------------------------------------------------------

JAVA-INSTALLED := $(JAVA) $(MAVEN)

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
OA-macos-arm64 := macos-arm64
OA-macos-int64 := macos-x64
OA-freebsd-int64 := freebsd-x64
OA-windows-arm64 := windows-arm64
OA-windows-int64 := windows-x64

RELEASE_PLATFORM ?= $(OA-$(OS-ARCH))
RELEASE-ENGINE-SUFFIX := \
  $(if $(filter graalvm,$(YAMLSCRIPT_ENGINE)),-graalvm)
RELEASE-YS-NAME := \
  ys-$(YS_VERSION)-$(RELEASE_PLATFORM)$(RELEASE-ENGINE-SUFFIX)
RELEASE-EXT := \
  $(if $(findstring windows,$(RELEASE_PLATFORM)),zip,tar.xz)
RELEASE-YS-TAR := $(RELEASE-YS-NAME).$(RELEASE-EXT)

RELEASE-LYS-NAME := \
  libys-$(YS_VERSION)-$(RELEASE_PLATFORM)$(RELEASE-ENGINE-SUFFIX)
RELEASE-LYS-TAR := $(RELEASE-LYS-NAME).$(RELEASE-EXT)


#------------------------------------------------------------------------------
default::
