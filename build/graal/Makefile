SHELL := bash

ifneq (,$(wildcard vars.mk))
include vars.mk
endif

version := $(YS-VERSION)

# If we are building in the YS repo:
ifneq (,$(wildcard ../the-ys-repo-build-dir))
REPO-BUILD := true
MAKES := ../../.cache/makes
MAKES-LOCAL-DIR := /tmp/ys-build-makes-local
ROOT := $(shell cd ../../ && pwd -P)
MAVEN-HOME := $(MAKES-LOCAL-DIR)/home
MAKES-QUIET := $(if $(YS_BUILD_DEBUG),,true)

# We are in the standalone build directory:
else
STAND-ALONE-BUILD := true
MAKES := ./.cache/makes
MAKES-LOCAL-DIR := /tmp/ys-build-makes-local
ROOT := $(shell pwd -P)
MAVEN-HOME := $(ROOT)
endif

export MAVEN_HOME := $(MAVEN-HOME)

$(shell [[ -d $(MAKES) ]] || \
  git clone -q https://github.com/makeplus/makes $(MAKES))

include $(MAKES)/init.mk
include $(MAKES)/graalvm.mk
include $(MAKES)/lein.mk
include $(MAKES)/shell.mk

YS-CORE-JAR-FILE := \
  $(MAVEN-HOME)/.m2/repository/yamlscript/core/$(version)/core-$(version).jar
BUILD-JAR-FILE := target/uberjar/$(CLJ-MAIN-NAMESPACE)-build-standalone.jar

NATIVE-OPTIONS := \
  -O1 \
  --verbose \
  --no-fallback \
  --initialize-at-build-time \
	--initialize-at-run-time=clojure.lang.Compiler \
  --enable-preview \
  --enable-url-protocols=https \
  -march=compatibility \
  -H:+UnlockExperimentalVMOptions \
  -H:+StaticExecutableWithDynamicLibC \
  -H:IncludeResources=SCI_VERSION \
  -H:ReflectionConfigurationFiles=reflection.json \
  -H:+ReportExceptionStackTraces \
  -H:Log=registerResource: \
  -J-Dclojure.spec.skip-macros=true \
  -J-Dclojure.compiler.direct-linking=true \
  -J-Xmx3g \

BUILD-DEPS := \
	$(BUILD-JAR-FILE) \
	reflection.json \
	.gitignore \

JAR-DEPS := \
	src/$(CLJ-MAIN-FILE) \
	project.clj \
	$(LEIN) \

ifdef REPO-BUILD
JAR-DEPS += \
	$(YS-CORE-JAR-FILE)
endif

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

build: $(BUILD-DEPS) $(GRAALVM)
	$Q $(LOG) "* Building the GraalVM native-image binary executable..."
	$Q $(LOG) "  This may take a few minutes. Steps 2 and 6 take the longest."
	$Q native-image \
	  $(NATIVE-OPTIONS) \
	  -jar $(BUILD-JAR-FILE) \
	  -o "$(YS-INPUT-NAME)" |& \
	if [[ '$(YS_BUILD_DEBUG)' || '$(STAND-ALONE-BUILD)' ]]; then \
		cat; \
	elif [[ '$(YS_BUILD_QUIET)' ]]; then \
		cat > /dev/null; \
	else \
		grep -E '^\['; \
	fi
	$Q $(STRIP) '$(YS-INPUT-NAME)'
ifdef REPO-BUILD
	$Q cp -r $(MAVEN-HOME)/.m2 ./
endif

$(BUILD-JAR-FILE): $(JAR-DEPS)
	$Q lein deps $O
	$Q lein uberjar $O

$(YS-CORE-JAR-FILE):
ifdef REPO-BUILD
	$Q $(MAKE) -C $(ROOT)/core install $O
endif

ifdef REPO-BUILD
reflection.json:
	$Q cp $(ROOT)/common/reflection.json $@
endif

define clj_head
(ns $(CLJ-MAIN-NAMESPACE)
  (:gen-class)
  (:refer-clojure
   :exclude [atom eval print read replace reverse set])
  (:require
	 [ys.std :as std :refer :all]
	 [ys.dwim :refer :all]
	 [ys.fs :as fs]
   [ys.http :as http]
   [ys.json :as json]
   [ys.yaml :as yaml]))

(defn map-parse [argv]
  (mapv #(if (re-matches #"-?\d+" %) (parse-long %) %) argv))
endef
export clj_head

define clj_tail
(defn -main [& argv] (apply main (map-parse argv)))
endef
export clj_tail

src/$(CLJ-MAIN-FILE): src/$(YS-INPUT-FILE-NAME)
	$Q (echo ";; THIS IS A GENERATED FILE. DO NOT EDIT."; \
	  echo; \
	  echo "$$clj_head"; \
	  echo; \
	  $(ys) -c $< | \
	  $(ys) -e 'IN:read.replace(/\(apply main ARGS\)\n/):print'; \
	  echo; \
	  echo "$$clj_tail"; \
	) > $@

ifdef REPO-BUILD
project.clj: ../share/project.clj
	$Q $(ys) -e 'IN:read.replace(/NAME/ "$(CLJ-MAIN-NAMESPACE)"):print' \
	  < $< > $@
endif

ifdef REPO-BUILD
.gitignore: ../share/graal-gitignore
	$Q $(ys) -e 'IN:read.replace(/NAME/ "$(YS-INPUT-NAME)"):print' \
	  < $< > $@
endif

ifdef REPO-BUILD
src/$(YS-INPUT-FILE-NAME): $(YS-INPUT-FILE) | src
	$Q cp $< $@

src:
	$Q mkdir -p $@
endif
