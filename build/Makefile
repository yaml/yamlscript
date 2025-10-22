MAKES-QUIET := $(if $(YS_BUILD_DEBUG),,true)

M := ../.cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)
include $M/init.mk

ifndef YS_BUILD_TYPE
  $(error YS_BUILD_TYPE is not set)
endif
ifndef YS_INPUT_PATH
  $(error YS_INPUT_PATH is not set)
endif
ifndef YS_OUTPUT_PATH
  $(error YS_OUTPUT_PATH is not set)
endif
ifndef YS_CLI_BINARY
  $(error YS_CLI_BINARY is not set)
endif

in-path := $(YS_INPUT_PATH)
out-path := $(YS_OUTPUT_PATH)
in-type := $(shell [[ -d $(in-path) ]] && echo 'dir'; \
                   [[ -f $(in-path) ]] && echo 'file')

ifndef in-type
  $(error Input path does not exist: $(in-path))
endif
$(shell [[ -f $(out-path) ]] && rm -f $(out-path))
ifneq (,$(wildcard $(out-path)))
ifneq (/dev/stdout,$(out-path))
  $(error Won't overwrite output path: $(out-path))
endif
endif
out-type := $(shell [[ $(out-path) == */ ]] && echo 'dir' || echo 'file')

YS_INPUT_TYPE := $(in-type)
YS_OUTPUT_TYPE := $(out-type)

YS_INPUT_FILE := $(YS_INPUT_PATH)
ifeq ($(in-type),dir)
YS_INPUT_FILE := \
  $(shell (cd $(in-path) && find . -type f | head -n1 | cut -d/ -f2-))
endif
YS_INPUT_FILE_NAME := $(notdir $(YS_INPUT_FILE))
YS_INPUT_NAME := $(basename $(YS_INPUT_FILE_NAME))
YS_BUILD_TARGET := $(YS_INPUT_NAME)

CLJ_MAIN_NAMESPACE := main-$(YS_INPUT_NAME)
CLJ_MAIN_NAME := $(subst -,_,$(CLJ_MAIN_NAMESPACE))
CLJ_MAIN_FILE := $(CLJ_MAIN_NAME).clj

# Put env vars into a file so they can be used by the build Makefile
define vars_mk
YS-VERSION := $(YS_VERSION)
YS-BUILD-TYPE := $(YS_BUILD_TYPE)
YS-BUILD-TARGET := $(YS_BUILD_TARGET)
YS-CLI-BINARY := $(YS_CLI_BINARY)
YS-INPUT-PATH := $(YS_INPUT_PATH)
YS-INPUT-TYPE := $(YS_INPUT_TYPE)
YS-INPUT-FILE := $(YS_INPUT_FILE)
YS-INPUT-FILE-NAME := $(YS_INPUT_FILE_NAME)
YS-INPUT-NAME := $(YS_INPUT_NAME)
YS-OUTPUT-PATH := $(YS_OUTPUT_PATH)
YS-OUTPUT-TYPE := $(YS_OUTPUT_TYPE)
CLJ-MAIN-NAMESPACE := $(CLJ_MAIN_NAMESPACE)
CLJ-MAIN-NAME := $(CLJ_MAIN_NAME)
CLJ-MAIN-FILE := $(CLJ_MAIN_FILE)
endef
export vars_mk

GLJ-MAIN-FILE := $(CLJ-MAIN-FILE:%.clj=%.glj)

build: clean
	@echo "$$vars_mk" > $(YS_BUILD_TYPE)/vars.mk
	$Q $(MAKE) --no-print-directory -C $(YS_BUILD_TYPE) $@
ifeq (/dev/stdout,$(YS_OUTPUT_PATH))
	$Q cat $(YS_BUILD_TYPE)/$(YS_BUILD_TARGET)
else ifeq (dir,$(YS_OUTPUT_TYPE))
	$Q cp -r $(YS_BUILD_TYPE) $(YS_OUTPUT_PATH)
	$Q git -C $(YS_OUTPUT_PATH) init $O
	$Q git -C $(YS_OUTPUT_PATH) add . $O
	$Q git -C $(YS_OUTPUT_PATH) commit -m "Initial commit" $O
else ifeq (file,$(YS_OUTPUT_TYPE))
ifeq (go,$(shell [[ $(YS_OUTPUT_PATH) == *.go ]] && echo go))
	$Q cp -p $(YS_BUILD_TYPE)/$(CLJ_MAIN_NAME)/loader.go $(YS_OUTPUT_PATH)
# else ifeq (go,$(shell [[ $(YS_OUTPUT_PATH) == *.glj ]] && echo glj))
#   $Q cp -p $(YS_BUILD_TYPE)/$(GLJ_MAIN_NAME) $(YS_OUTPUT_PATH)
else
	$Q cp -p $(YS_BUILD_TYPE)/$(YS_BUILD_TARGET) $(YS_OUTPUT_PATH)
endif
else
	$(error Unknown output type: '$(YS_OUTPUT_PATH)')
endif

clean:
	$Q git clean -xdf . $O
