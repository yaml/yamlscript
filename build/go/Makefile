SHELL := bash

ifneq (,$(wildcard vars.mk))
include vars.mk
endif

version := $(YS-VERSION)

GO-VERSION := 1.19.3

# If we are building in the YS repo:
ifneq (,$(wildcard ../the-ys-repo-build-dir))
REPO-BUILD := true
MAKES := ../../.cache/makes
MAKES-LOCAL-DIR := /tmp/ys-build-makes-local
MAKES-LOCAL-DIR := /home/ingy/src/ys-build-makes-local
ROOT := $(shell cd ../../ && pwd -P)
MAKES-QUIET := $(if $(YS_BUILD_DEBUG),,true)

# We are in the standalone build directory:
else
STAND-ALONE-BUILD := true
MAKES := ./.cache/makes
MAKES-LOCAL-DIR := /tmp/ys-build-makes-local
ROOT := $(shell pwd -P)
endif

$(shell [[ -d $(MAKES) ]] || \
  git clone -q https://github.com/makeplus/makes $(MAKES))

include $(MAKES)/init.mk
include $(MAKES)/babashka.mk
include $(MAKES)/glojure.mk
include $(MAKES)/go.mk
include $(MAKES)/clean.mk
include $(MAKES)/shell.mk

GO-MAIN-FILE := $(CLJ-MAIN-FILE:%.clj=%/loader.go)

MAKES-CLEAN := $(YS-BUILD-TARGET)

BUILD-DEPS := \
	.gitignore \
	go.mod \
	main.go \
	ys/v0.glj \
	$(GO-MAIN-FILE) \

ys := $(YS-CLI-BINARY)

ifdef YS_BUILD_QUIET
LOG := :
else
LOG := $(if $(YS_BUILD_DEBUG),:,echo)
endif

STRIP := $(if $(shell command -v strip),strip,:)

ifdef YS_BUILD_DEBUG
$(shell cat vars.mk >&2)
endif

build: $(BUILD-DEPS) $(GO)
	$Q go mod tidy
	$Q go build -o $(YS-BUILD-TARGET) main.go
	$Q $(STRIP) $(YS-BUILD-TARGET)

define clj_head
(ns $(CLJ-MAIN-NAMESPACE)
  (:require [ys.v0 :refer :all]))

(def ^:dynamic ARGV [])
(def ^:dynamic ARGS [])
;; (def ^:dynamic ENV {})
endef
export clj_head

define clj_tail
(defn -main [& argv]
  (let [args (map-parse argv)]
    (alter-var-root #'ARGV (constantly argv))
    (alter-var-root #'ARGS (constantly args))
		;; System/getenv is not available in Glojure
    ;; (alter-var-root #'ENV (constantly (into {} (System/getenv))))
    (apply main args)))
endef
export clj_tail

$(CLJ-MAIN-FILE): $(YS-INPUT-FILE-NAME)
	$Q (echo ";; THIS IS A GENERATED FILE. DO NOT EDIT."; \
	  echo; \
	  echo "$$clj_head"; \
	  echo; \
	  $(ys) -c $< | \
	  $(ys) -e 'IN:read.replace(/\(apply main ARGS\)\n/):print' | \
		$(ys) -e 'IN:read.replace(/ys\.std\//):print'; \
	  echo; \
	  echo "$$clj_tail"; \
	) > $@

GLJ-MAIN-FILE := $(CLJ-MAIN-FILE:%.clj=%.glj)
GLJ-MAIN-NAMESPACE := $(CLJ-MAIN-NAMESPACE)
GO-MAIN-PACKAGE := $(subst -,_,$(GLJ-MAIN-NAMESPACE))

$(GO-MAIN-FILE): $(GLJ-MAIN-FILE) $(GLJ)
	$Q glj <<<'(compile (quote $(GLJ-MAIN-NAMESPACE)))' $O
	$Q [[ -f $@ ]]

$(GLJ-MAIN-FILE): $(CLJ-MAIN-FILE) $(BB) $(GLOJURE-DIR)
	$Q bb $(GLOJURE-DIR)/scripts/rewrite-core/rewrite.clj $< > $@

ys/v0.glj: ../share/ys-v0.glj
	$Q mkdir -p $(dir $@)
	$Q cp $< $@

ifdef REPO-BUILD
go.mod: ../share/go.mod
	$Q $(ys) -e \
	  'IN:read.replace(/GLJ-MAIN-NAMESPACE/ "$(GLJ-MAIN-NAMESPACE)"):print' \
	  < $< > $@
endif

ifdef REPO-BUILD
main.go: ../share/main.go
	$Q < $< $(ys) -e \
	  'IN:read.replace(/GO-MAIN-PACKAGE/ "$(GO-MAIN-PACKAGE)"):print' | \
	$(ys) -e \
	  'IN:read.replace(/GLJ-MAIN-NAMESPACE/ "$(GLJ-MAIN-NAMESPACE)"):print' \
	> $@
endif

ifdef REPO-BUILD
.gitignore: ../share/graal-gitignore
	$Q $(ys) -e 'IN:read.replace(/NAME/ "$(YS-BUILD-TARGET)"):print' \
	  < $< > $@
endif

ifdef REPO-BUILD
$(YS-INPUT-FILE-NAME): $(YS-INPUT-FILE)
	$Q cp $< $@
endif
