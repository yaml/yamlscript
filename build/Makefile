include ../common/base.mk
include $(COMMON)/native.mk
include $(MAKES)/babashka.mk
include $(MAKES)/go.mk
include $(MAKES)/shell.mk

ifndef YS_BUILD_DEBUG
QUIET := @
NULL := &>/dev/null
endif

GLOJURE-REPO := https://github.com/yaml/glojure
GLOJURE-BRANCH := yamlscript-build

export GLOJURE := ./glojure
export native_options := \
  $(filter-out \
    --emit=build-report \
    --native-image-info \
    ,$(NATIVE-OPTIONS))

export wrap_bin := bin/wrap-$(BUILD-TYPE)

export ys_binary := $(YS-BINARY)
export input_path := $(INPUT-PATH)
export output_path := $(OUTPUT-PATH)
export ys_binary := $(YS-BINARY)
export build_type := $(BUILD-TYPE)

override export PATH := $(ROOT)/build/bin:$(PATH)


build-glj: glojure babashka
	$(QUIET)$@

build-go: glojure go
	$(QUIET)$@

build-graal-bin: lein
	$(QUIET)$@

build-go-bin: glojure go
	$(QUIET)$@

build-wasm: glojure go
	$(QUIET)$@

go:
	$(QUIET)$(MAKE) $(GO) $(NULL)

glojure:
	$(QUIET)$(MAKE) $(GLOJURE) $(NULL)

lein:
	$(QUIET)$(MAKE) $(LEIN) $(NULL)

babashka:
	$(QUIET)$(MAKE) $(BABSHKA) $(NULL)
