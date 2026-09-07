include $(MAKES)/gloat.mk
include $(MAKES)/perl.mk

GLOAT-RUN := $(GLOAT-BIN)/gloat
ifeq ($(OS-NAME),windows)
GLOAT-RUN := /usr/bin/bash $(GLOAT-BIN)/gloat
endif

GLOAT-ENGINE ?= glj
GLOAT-PLATFORM-OPT := \
  $(if $(GLOAT_PLATFORM),--platform=$(GLOAT_PLATFORM))
GLOAT-WINDOWS-TARGET := $(filter windows/%,$(GLOAT_PLATFORM))
GLOAT-WASI-TARGET := $(filter wasip1/wasm,$(GLOAT_PLATFORM))

GO-YAML-REF ?= 643e93b9c9bec2be8ae2b842a3492c54532549c9
GO-YAML-REPO ?= https://github.com/yaml/go-yaml
GO-YAML-SRC-DIR := $(ROOT)/.cache/glojure-parser/go-yaml
GO-YAML-STAMP := $(GO-YAML-SRC-DIR)/.fetched
GO-YAML-GENERATED-DIR := $(ROOT)/internal/goyamlparser
GO-YAML-GENERATED-STAMP := $(GO-YAML-GENERATED-DIR)/.generated
GO-YAML-PREPARE := $(ROOT)/core/gloat/prepare-go-yaml-parser

GLOJURE-SRC-DIR := $(ROOT)/.cache/glojure-src
GLOJURE-PREPARED-STAMP := $(GLOJURE-SRC-DIR)/.prepared
GLOJURE-SRC-PREPARE := $(ROOT)/core/gloat/prepare-clojure-source
GLOJURE-STDLIB-DIR := \
  $(GLOAT-DIR)/.cache/local/cache/glojure-$(GLOJURE-VERSION)/pkg/stdlib
TOOLS-CLI-VERSION := 1.0.219
TOOLS-CLI-JAR := \
  $(LOCAL-CACHE)/tools.cli-$(TOOLS-CLI-VERSION).jar
TOOLS-CLI-MAVEN := \
  https://repo1.maven.org/maven2/org/clojure/tools.cli
TOOLS-CLI-FILE := tools.cli-$(TOOLS-CLI-VERSION).jar
TOOLS-CLI-URL := \
  $(TOOLS-CLI-MAVEN)/$(TOOLS-CLI-VERSION)/$(TOOLS-CLI-FILE)
GLOJURE-TOOLS-CLI-CLJC := \
  $(GLOJURE-SRC-DIR)/vendor/clojure/tools/cli.cljc
GLOJURE-TOOLS-CLI-SRC := \
  $(GLOJURE-SRC-DIR)/vendor/clojure/tools/cli.clj
GLOJURE-COMPILER-NAMES := \
  re ast composer resolver ysreader builder transformers transformer \
  constructor printer
GLOJURE-COMPILER-SRCS := \
  $(GLOJURE-COMPILER-NAMES:%=$(GLOJURE-SRC-DIR)/yamlscript/%.clj)
GLOJURE-IPC-SRC := $(GLOJURE-SRC-DIR)/ys/v0/ipc.clj
GLOJURE-GLOBAL-SRC := $(GLOJURE-SRC-DIR)/ys/v0/global.clj
GLOJURE-PPRINT-SRC := $(GLOJURE-SRC-DIR)/ys/v0/pprint.clj
GLOJURE-TAPTEST-SRC := \
  $(GLOJURE-SRC-DIR)/yamlscript/module/taptest.clj
GLOJURE-DEPS-CLJC-NAMES := \
  grenadine/version \
  grenadine/gitlibs \
  grenadine/xml_tree \
  grenadine/lock \
  grenadine/repo \
  grenadine/coordinate \
  grenadine/expander \
  grenadine/graph \
  grenadine/pom \
  grenadine/basis \
  grenadine/core \
  grenadine/runtime \
  grenadine/require_deps \
  grenadine/xml \
  clojurestar/deps
GLOJURE-DEPS-CLJC-SRCS := \
  $(GLOJURE-DEPS-CLJC-NAMES:%=$(GLOJURE-SRC-DIR)/vendor/%.clj)
GLOJURE-DEPS-FACADE := \
  $(GLOJURE-SRC-DIR)/vendor/glojure/deps.clj
GLOJURE-DEPS-SRCS := \
  $(GLOJURE-DEPS-CLJC-SRCS) \
  $(GLOJURE-DEPS-FACADE)

GLOJURE-SRCS := \
  $(ROOT)/core/src-glojure/clojure/math.clj \
  $(ROOT)/core/src-glojure/clojure/set.clj \
  $(GLOJURE-TOOLS-CLI-SRC) \
  $(GLOJURE-DEPS-SRCS) \
  $(GLOJURE-GLOBAL-SRC) \
  $(GLOJURE-IPC-SRC) \
  $(GLOJURE-PPRINT-SRC) \
  $(GLOJURE-TAPTEST-SRC) \
  $(ROOT)/core/src-glojure/yamlscript/module/csv.clj \
  $(ROOT)/core/src-glojure/yamlscript/module/fs.clj \
  $(ROOT)/core/src-glojure/yamlscript/module/http.clj \
  $(ROOT)/core/src-glojure/yamlscript/module/io.clj \
  $(ROOT)/core/src-glojure/yamlscript/module/pprint.clj \
  $(ROOT)/core/src-glojure/yamlscript/module/pods.clj \
  $(ROOT)/core/src-glojure/yamlscript/global.clj \
  $(ROOT)/core/src-glojure/yamlscript/parser.clj \
  $(ROOT)/core/src-glojure/yamlscript/process.clj \
  $(ROOT)/core/src-glojure/yamlscript/regex.clj \
  $(GLOJURE-COMPILER-SRCS) \
  $(ROOT)/core/src-glojure/yamlscript/compiler.clj \
  $(ROOT)/core/src-glojure/yamlscript/runtime.clj

$(GO-YAML-STAMP):
	rm -rf '$(GO-YAML-SRC-DIR).tmp'
	mkdir -p $(dir $(GO-YAML-SRC-DIR))
	git init '$(GO-YAML-SRC-DIR).tmp'
	git -C '$(GO-YAML-SRC-DIR).tmp' fetch --depth=1 \
	  '$(GO-YAML-REPO)' '$(GO-YAML-REF)'
	git -C '$(GO-YAML-SRC-DIR).tmp' checkout --detach FETCH_HEAD
	rm -rf '$(GO-YAML-SRC-DIR)'
	mv '$(GO-YAML-SRC-DIR).tmp' '$(GO-YAML-SRC-DIR)'
	touch '$@'

$(GO-YAML-GENERATED-STAMP): \
  $(GO-YAML-STAMP) $(GO-YAML-PREPARE) $(PERL)
	PERL=$(PERL) bash $(GO-YAML-PREPARE) \
	  $(GO-YAML-SRC-DIR) $(GO-YAML-GENERATED-DIR)
	touch '$@'

$(GLOJURE-SRC-DIR)/yamlscript/%.clj: \
  $(ROOT)/core/src/yamlscript/%.clj $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-IPC-SRC): \
  $(ROOT)/core/src/ys/v0/ipc.cljc $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-GLOBAL-SRC): \
  $(ROOT)/core/src/ys/v0/global.clj $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-PPRINT-SRC): \
  $(ROOT)/core/src/ys/v0/pprint.cljc $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-TAPTEST-SRC): \
  $(ROOT)/core/src/ys/v0/taptest.clj $(GLOJURE-SRC-PREPARE) $(PERL)
	mkdir -p $(dir $@)
	$(PERL) -pe \
	  's/^\(ns ys\.v0\.taptest$$/(ns yamlscript.module.taptest/' \
	  $< > $@

$(GLOJURE-DEPS-CLJC-SRCS): \
  $(GLOJURE-SRC-DIR)/vendor/%.clj: \
  $(GLOAT) $(GLOJURE-SRC-PREPARE) $(PERL) \
  $(ROOT)/common/glojure.mk
	test -f $(GLOJURE-STDLIB-DIR)/$*.cljc
	$(PERL) $(GLOJURE-SRC-PREPARE) \
	  $(GLOJURE-STDLIB-DIR)/$*.cljc $@

$(GLOJURE-DEPS-FACADE): \
  $(GLOAT) $(GLOJURE-SRC-PREPARE) $(PERL) \
  $(ROOT)/common/glojure.mk
	test -f $(GLOJURE-STDLIB-DIR)/glojure/deps.clj
	mkdir -p $(dir $@)
	$(PERL) -0pe \
	  's/\s*\[glojure\.deps\.host :as host\]//' \
	  $(GLOJURE-STDLIB-DIR)/glojure/deps.clj | \
	  $(PERL) -pe 's/\bhost\//glojure.deps.host\//g' > $@

$(TOOLS-CLI-JAR):
	$(call need-curl)
	mkdir -p $(dir $@)
	$(CURL) -f $(TOOLS-CLI-URL) > $@

$(GLOJURE-TOOLS-CLI-CLJC): \
  $(TOOLS-CLI-JAR) $(ROOT)/common/glojure.mk $(PERL)
	mkdir -p $(dir $@)
	unzip -p $< clojure/tools/cli.cljc | \
	  $(PERL) -0pe \
	    's/\(ns\s+\^\{.*?\}\s+clojure\.tools\.cli/(ns clojure.tools.cli/s' \
	    > $@

$(GLOJURE-TOOLS-CLI-SRC): \
  $(GLOJURE-TOOLS-CLI-CLJC) $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-PREPARED-STAMP): \
  $(GLOJURE-SRCS) $(GO-YAML-GENERATED-STAMP)
	touch $@

prepare-glojure: $(GLOJURE-PREPARED-STAMP)
