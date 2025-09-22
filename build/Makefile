MAKES-QUIET := $(if $(YS_BUILD_DEBUG),,true)

include ../common/base.mk
include $(COMMON)/native.mk
include $(MAKES)/babashka.mk
GO-VERSION ?= 1.19.3
include $(MAKES)/go.mk
include $(MAKES)/glojure.mk
include $(MAKES)/graalvm.mk
include $(MAKES)/shell.mk

LOG := $(if $(YS_BUILD_QUIET),:,echo)

export GLOJURE_DIR := $(GLOJURE-DIR)

export native_options := \
  $(filter-out \
    --emit=build-report \
    --native-image-info \
    ,$(NATIVE-OPTIONS))

export wrap_bin := bin/wrap-$(YS_BUILD_TYPE)

override export PATH := \
  $(ROOT)/build/bin:$(abspath $(GLOJURE-DIR))/bin:$(PATH)

export YS_INPUT_NAME := $(basename $(notdir $(YS_INPUT_PATH)))


build-glj: $(BB) $(GLOJURE-DIR)
	$Q $@

build-go: $(BB) $(GO) glj $(GLOJURE-DIR)
	$Q $@

build-go-bin: $(BB) $(GRAALVM) $(GO) glj $(GLOJURE-DIR)
ifdef MAKES-QUIET
	@$(LOG) "* Compiling the Go binary executable"
endif
	$Q $(YS_CLI_BINARY) bin/$@
	$(call maybe-stdout)

build-graal-bin: $(LEIN)
	$Q $@

build-wasm: $(GO) $(GLOJURE-DIR)
	$Q $@

glj: glj-announce $(GLJ)


glj-announce:
ifdef MAKES-QUIET
	@$(LOG) "* Installing Glojure 'glj' command in build directory"
endif

ifeq ($(YS_OUTPUT_PATH),/dev/stdout)
define maybe-stdout
	$Q cat /tmp/ys-build-stdout
endef
endif
