GLOAT-VERSION := 0.1.91
include $(MAKES)/gloat.mk
GLOJURE-VERSION := 0.7.24
include $(MAKES)/glojure.mk
GO-VERSION := 1.24.0
ifdef IS-MACOS
GO-VERSION := 1.25.7
endif
include $(MAKES)/go.mk
include $(MAKES)/perl.mk

PERL-CMD := $(PERL)
ifeq ($(OS-NAME),windows)
# Makes uses $(PERL) as a system-Perl availability sentinel on Windows.
PERL-CMD := perl
endif

GLOAT-RUN-DEPS := $(GLOAT)
GLOAT-RUN := $(GLOAT-BIN)/gloat
ifeq ($(OS-NAME),windows)
# Gloat cannot find tools installed during its first Windows run.
# Populate its managed cache and prepare its Glojure binary for Windows.
GLOAT-WINDOWS-PATH-DEPS := $(GLOAT-DIR)/.cache/.windows-path-deps
GLOAT-RUN-DEPS += $(GLOAT-WINDOWS-PATH-DEPS)
GLOAT-RUN := \
  PATH="$$(cat $(GLOAT-WINDOWS-PATH-DEPS)):/usr/bin:$$PATH" \
  /usr/bin/bash $(GLOAT-BIN)/gloat

$(GLOAT-WINDOWS-PATH-DEPS): $(GLOAT) $(ROOT)/common/glojure.mk
	TAR_OPTIONS=--force-local \
	  $(GLOAT-PINNED-MAKE) --quiet -C $(GLOAT-DIR) \
	  path-bb path-go path-glj >/dev/null
	gloat_dir=$$(cd $(GLOAT-DIR) && pwd -P); \
	  cache_path=; \
	  for bin_dir in "$$gloat_dir"/.cache/local/*/bin; do \
	    [[ -d "$$bin_dir" ]] || continue; \
	    if [[ -f "$$bin_dir/glj" ]]; then \
	      cp -p "$$bin_dir/glj" "$$bin_dir/glj.exe"; \
	    fi; \
	    cache_path=$${cache_path:+$$cache_path:}$$bin_dir; \
	  done; \
	  [[ $$cache_path ]]; \
	  printf '%s\n' "$$cache_path" > $@
endif

GLOAT-ENGINE ?= glj
GLOAT-PLATFORM-OPT := \
  $(if $(GLOAT_PLATFORM),--platform=$(GLOAT_PLATFORM))
GLOAT-WINDOWS-TARGET := $(filter windows/%,$(GLOAT_PLATFORM))
GLOAT-WASI-TARGET := $(filter wasip1/wasm,$(GLOAT_PLATFORM))

GO-YAML-REF ?= 14f6a7261be3104f473c4ca93e3abf304c29a740
GO-YAML-REPO ?= https://github.com/yaml/go-yaml
GO-YAML-SRC-DIR := $(ROOT)/.cache/glojure-parser/go-yaml-$(GO-YAML-REF)
GO-YAML-STAMP := $(GO-YAML-SRC-DIR)/.fetched
GO-YAML-GENERATED-DIR := $(ROOT)/internal/goyamlparser
GO-YAML-GENERATED-STAMP := $(GO-YAML-GENERATED-DIR)/.generated
GO-YAML-PREPARE := $(ROOT)/core/gloat/prepare-go-yaml-parser

GLOJURE-SRC-DIR := $(ROOT)/.cache/glojure-src
GLOJURE-PREPARED-STAMP := $(GLOJURE-SRC-DIR)/.prepared
GLOJURE-SRC-PREPARE := $(ROOT)/core/gloat/prepare-clojure-source
GLOJURE-REF ?= v$(GLOJURE-VERSION)
GLOJURE-REPO ?= https://github.com/glojurelang/glojure
GLOJURE-UPSTREAM-DIR := \
  $(ROOT)/.cache/glojure-source/glojure-$(GLOJURE-VERSION)
GLOJURE-UPSTREAM-STAMP := $(GLOJURE-UPSTREAM-DIR)/.fetched
GLOJURE-STDLIB-DIR := $(GLOJURE-UPSTREAM-DIR)/pkg/stdlib

# Always prepare the runtime from this YAMLScript checkout. This picks up
# the version bump performed by make release without needing a published JAR.
YS-V0-GLJ-REF := 5d9391ae4d8adc449fe20c28e3b64cf6d876ad29
YS-V0-GLJ-REPO ?= https://github.com/gloathub/ys-v0-glj
YS-V0-SOURCE ?= $(ROOT)/core/src
GLOJURE-BUILD-DIR := $(or $(GLOJURE_DIR),$(GLOJURE-UPSTREAM-DIR))
GLOJURE-BUILD-REF := $(if $(GLOJURE_DIR),$(shell \
  git -C '$(GLOJURE_DIR)' rev-parse HEAD),$(GLOJURE-VERSION))
GLOJURE-BUILD-GLJ := $(or $(wildcard \
  $(GLOJURE_DIR)/bin/$(subst /,_,$(HOST-GLOAT-PLATFORM))/glj \
  $(GLOJURE_DIR)/bin/$(subst /,_,$(HOST-GLOAT-PLATFORM))/glj.exe),$(GLJ))
YS-V0-GLJ-CACHE-KEY := $(YS-V0-GLJ-REF)-$(YS_VERSION)-$(GLOJURE-BUILD-REF)
YS-V0-GLJ-DIR ?= $(ROOT)/.cache/ys-v0-glj/$(YS-V0-GLJ-CACHE-KEY)
YS-V0-GLJ-FETCHED := $(YS-V0-GLJ-DIR)/.fetched
YS-V0-GLJ-PREPARED := $(YS-V0-GLJ-DIR)/.prepared
YS-V0-SOURCES := \
  $(wildcard $(YS-V0-SOURCE)/ys/*.clj*) \
  $(wildcard $(YS-V0-SOURCE)/ys/v0/*.clj*) \
  $(YS-V0-SOURCE)/yamlscript/regex.cljc

# Explicit command-line variables are inherited by Gloat's own make process.
# Keep these overrides on the compiler invocation, not on unrelated submakes.
GLOAT-PINNED-MAKE = $(MAKE) --no-print-directory \
  GLOJURE-VERSION=$(GLOJURE-VERSION) \
  GLOJURE_DIR='$(GLOJURE-BUILD-DIR)' \
  YS-V0-GLJ-DIR='$(YS-V0-GLJ-DIR)' \
  YS-V0-GLJ-DEV-DIR='$(YS-V0-GLJ-DIR)'

$(YS-V0-GLJ-FETCHED):
	rm -rf '$(YS-V0-GLJ-DIR).tmp'
	mkdir -p '$(dir $(YS-V0-GLJ-DIR))'
	git init '$(YS-V0-GLJ-DIR).tmp'
	git -C '$(YS-V0-GLJ-DIR).tmp' fetch --depth=1 \
	  '$(YS-V0-GLJ-REPO)' '$(YS-V0-GLJ-REF)'
	test "$$(git -C '$(YS-V0-GLJ-DIR).tmp' rev-parse FETCH_HEAD)" \
	  = '$(YS-V0-GLJ-REF)'
	git -C '$(YS-V0-GLJ-DIR).tmp' checkout --detach FETCH_HEAD
	rm -rf '$(YS-V0-GLJ-DIR)'
	mv '$(YS-V0-GLJ-DIR).tmp' '$(YS-V0-GLJ-DIR)'
	touch '$@'

$(YS-V0-GLJ-PREPARED): $(YS-V0-GLJ-FETCHED) $(YS-V0-SOURCES) \
  $(GLOJURE-UPSTREAM-STAMP) $(GLOJURE-BUILD-GLJ) $(GO) $(PERL) \
  $(ROOT)/Meta $(ROOT)/common/glojure.mk
	cd '$(YS-V0-GLJ-DIR)' && env -u GOROOT -u ROOT \
	  GLJ='$(abspath $(GLOJURE-BUILD-GLJ))' \
	  GLOJURE_SRC='$(GLOJURE-BUILD-DIR)' \
	  GO='$(abspath $(GO))' PERL='$(PERL-CMD)' \
	  YS_V0_VERSION='$(YS_VERSION)' YS_V0_SOURCE='$(YS-V0-SOURCE)' \
	  bash script/generate
	printf '%s\n' 'gloat=$(GLOAT-VERSION)' \
	  'glojure=$(GLOJURE-VERSION)' 'ys-v0-glj=$(YS-V0-GLJ-REF)' \
	  'yamlscript=$(YS_VERSION)' > '$@'

prepare-glojure-runtime: $(YS-V0-GLJ-PREPARED)

glojure-build-info: $(YS-V0-GLJ-PREPARED)
	@cat '$(YS-V0-GLJ-PREPARED)'
	@printf '%s\n' 'runtime-dir=$(YS-V0-GLJ-DIR)' \
	  'glojure-dir=$(GLOJURE-BUILD-DIR)'

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
  $(ROOT)/core/src/ys/v0/imports.clj \
  $(ROOT)/core/src-glojure/yamlscript/runtime.clj

$(GLOJURE-UPSTREAM-STAMP):
	rm -rf '$(GLOJURE-UPSTREAM-DIR).tmp'
	mkdir -p $(dir $(GLOJURE-UPSTREAM-DIR))
	git init '$(GLOJURE-UPSTREAM-DIR).tmp'
	git -C '$(GLOJURE-UPSTREAM-DIR).tmp' fetch --depth=1 \
	  '$(GLOJURE-REPO)' '$(GLOJURE-REF)'
	git -C '$(GLOJURE-UPSTREAM-DIR).tmp' checkout --detach FETCH_HEAD
	rm -rf '$(GLOJURE-UPSTREAM-DIR)'
	mv '$(GLOJURE-UPSTREAM-DIR).tmp' '$(GLOJURE-UPSTREAM-DIR)'
	touch '$@'

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
	PERL=$(PERL-CMD) bash $(GO-YAML-PREPARE) \
	  $(GO-YAML-SRC-DIR) $(GO-YAML-GENERATED-DIR)
	touch '$@'

$(GLOJURE-SRC-DIR)/yamlscript/%.clj: \
  $(ROOT)/core/src/yamlscript/%.clj $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL-CMD) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-IPC-SRC): \
  $(ROOT)/core/src/ys/v0/ipc.cljc $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL-CMD) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-GLOBAL-SRC): \
  $(ROOT)/core/src/ys/v0/global.clj $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL-CMD) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-PPRINT-SRC): \
  $(ROOT)/core/src/ys/v0/pprint.cljc $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL-CMD) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-TAPTEST-SRC): \
  $(ROOT)/core/src/ys/v0/taptest.clj $(GLOJURE-SRC-PREPARE) $(PERL) \
  $(ROOT)/common/glojure.mk
	mkdir -p $(dir $@)
	$(PERL-CMD) -pe \
	  's/^\(ns ys\.v0\.taptest\b/(ns yamlscript.module.taptest/' \
	  $< > $@

$(GLOJURE-DEPS-CLJC-SRCS): \
  $(GLOJURE-SRC-DIR)/vendor/%.clj: \
  $(GLOJURE-UPSTREAM-STAMP) $(GLOJURE-SRC-PREPARE) $(PERL) \
  $(ROOT)/common/glojure.mk
	test -f $(GLOJURE-STDLIB-DIR)/$*.cljc
	$(PERL-CMD) $(GLOJURE-SRC-PREPARE) \
	  $(GLOJURE-STDLIB-DIR)/$*.cljc $@

$(GLOJURE-DEPS-FACADE): \
  $(GLOJURE-UPSTREAM-STAMP) $(GLOJURE-SRC-PREPARE) $(PERL) \
  $(ROOT)/common/glojure.mk
	test -f $(GLOJURE-STDLIB-DIR)/glojure/deps.clj
	mkdir -p $(dir $@)
	$(PERL-CMD) -0pe \
	  's/\s*\[glojure\.deps\.host :as host\]//' \
	  $(GLOJURE-STDLIB-DIR)/glojure/deps.clj | \
	  $(PERL-CMD) -pe 's/\bhost\//glojure.deps.host\//g' > $@

$(TOOLS-CLI-JAR):
	$(call need-curl)
	mkdir -p $(dir $@)
	$(CURL) -f $(TOOLS-CLI-URL) > $@

$(GLOJURE-TOOLS-CLI-CLJC): \
  $(TOOLS-CLI-JAR) $(ROOT)/common/glojure.mk $(PERL)
	mkdir -p $(dir $@)
	unzip -p $< clojure/tools/cli.cljc > $@
	$(PERL-CMD) -0pi -e \
	    's/\(ns\s+\^\{.*?\}\s+clojure\.tools\.cli/(ns clojure.tools.cli/s' \
	    $@

$(GLOJURE-TOOLS-CLI-SRC): \
  $(GLOJURE-TOOLS-CLI-CLJC) $(GLOJURE-SRC-PREPARE) $(PERL)
	$(PERL-CMD) $(GLOJURE-SRC-PREPARE) $< $@

$(GLOJURE-PREPARED-STAMP): \
  $(GLOJURE-SRCS) $(GO-YAML-GENERATED-STAMP)
	touch $@

prepare-glojure: $(GLOJURE-PREPARED-STAMP)
