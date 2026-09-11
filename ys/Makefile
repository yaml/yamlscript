include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/native.mk
include $(COMMON)/vars-cli.mk
include $(COMMON)/docker.mk
include $(COMMON)/glojure.mk

include $(MAKES)/perl.mk
ifneq (,$(filter test-compile-real,$(MAKECMDGOALS)))
export UV_CACHE_DIR := $(LOCAL-CACHE)/uv
export UV_PYTHON_INSTALL_BIN := 0
include $(MAKES)/python.mk
include $(MAKES)/node.mk
include $(MAKES)/wasmtime.mk
endif

include $(MAKES)/shell.mk

YS_COMPILE_REAL_GLOAT ?= $(abspath $(GLOAT-BIN)/gloat)

BPAN-LOCAL := .bpan
BPAN-REPO-URL := https://github.com/bpan-org/bpan

ifeq ($(OS-NAME),windows)
PROVE := $(PERL) -S prove
else
PROVE := prove
endif

test := test/
TEST-RUN-BASH := test/taptest-failures.t
TEST-RUN-YS := $(filter-out $(TEST-RUN-BASH),$(wildcard test/*.t))

YSJ-TMP := .cache/tmp/ysj
YSJ-BIN-DIR := $(YSJ-TMP)/bin
YSJ-BIN := $(YSJ-BIN-DIR)/ys
YSJ-API-BIN := $(YSJ-BIN-DIR)/ys-$(API_VERSION)
YSJ-DOCKER-CONFIG := $(YSJ-TMP)/docker

test-run: export PATH := $(abspath bin):$(PATH)


#------------------------------------------------------------------------------
build:: $(CLI-DEPS)

build-glojure:
	$(MAKE) build YAMLSCRIPT_ENGINE=glojure

build-graalvm:
	$(MAKE) build YAMLSCRIPT_ENGINE=graalvm

install: $(CLI-DEPS)
	mkdir -p $(PREFIX)/bin
	install -m 755 $(CLI-BIN) $(PREFIX)/bin/
ifneq ($(OS-NAME),windows)
	ln -fs $(notdir $(CLI-BIN)) \
	  $(PREFIX)/$(CLI-BIN:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $(CLI-BIN)) \
	  $(PREFIX)/$(CLI-BIN:%-$(YS_VERSION)=%)
endif
# The -T bb/clj/clj+ compile targets need the ys.v0 and data.json
# jars in ~/.m2:
	$(MAKE) -C $(ROOT)/v0 install

jar: $(CLI-JAR)

test: test-run

test-all: test-unit test-run

test-unit: $(LEIN) $(CORE-INSTALLED)
	lein test $(lein-test)

test-compile: $(CLI-DEPS) $(LEIN) $(CORE-INSTALLED)
	YS_COMPILE_TEST_BIN=$(abspath $(CLI-BIN)) \
	  lein test yamlscript.compile-test

test-expression: $(CLI-DEPS) $(LEIN) $(CORE-INSTALLED)
	YS_EXPRESSION_TEST_BIN=$(abspath $(CLI-BIN)) \
	  lein test yamlscript.expression-test

test-compile-real: $(CLI-DEPS) $(LEIN) $(CORE-INSTALLED) \
    $(GLOAT-RUN) $(PYTHON) $(NODE) $(WASMTIME)
	YS_COMPILE_TEST_BIN=$(abspath $(CLI-BIN)) \
	  YS_COMPILE_REAL_GLOAT=$(YS_COMPILE_REAL_GLOAT) \
	  YS_COMPILE_PYTHON=$(PYTHON) \
	  YS_COMPILE_NODE=$(NODE) YS_COMPILE_WASMTIME=$(WASMTIME) \
	  lein test :only yamlscript.compile-test/real-gloat-artifacts

test-install: $(CLI-DEPS) $(LEIN) $(CORE-INSTALLED)
ifeq ($(YAMLSCRIPT_ENGINE),graalvm)
	$(MAKE) build-glojure
endif
	YS_INSTALL_TEST_BIN=$(abspath $(CLI-BIN)) \
	  lein test yamlscript.util-test yamlscript.install-cli-test

ifdef YS_RELEASE_USE_INSTALLED_YS
TEST-RUN-DEPS := $(PERL) $(BPAN-LOCAL)
else
TEST-RUN-DEPS := $(PERL) $(CLI-DEPS) $(BPAN-LOCAL)
endif

test-run: $(TEST-RUN-DEPS)
ifdef YS_RELEASE_USE_INSTALLED_YS
	test -f $(CLI-BIN)
endif
	$(PROVE) --exec $(abspath $(CLI-BIN)) \
	  $${TEST_VERBOSE:+'-v'} $(TEST-RUN-YS)
	$(PROVE) $${TEST_VERBOSE:+'-v'} $(TEST-RUN-BASH)

test-ysj: export DOCKER_CONFIG := $(abspath $(YSJ-DOCKER-CONFIG))
test-ysj: export PATH := $(abspath $(YSJ-BIN-DIR)):$(PATH)
test-ysj: export YS_BIN_DIR := $(abspath $(YSJ-BIN-DIR))
test-ysj: $(PERL) $(YSJ-BIN) $(YSJ-API-BIN) $(YSJ-DOCKER-CONFIG) \
	  $(BPAN-LOCAL)
	$(PROVE) $${TEST_VERBOSE:+'-v'} $(test)

repl-deps:: $(CORE-INSTALLED)

clean::
	$(RM) -r .cpcache/
	$(RM) $(YS-BUILD-LOG)

realclean::
	$(RM) -r bin lib $(BPAN-LOCAL)

$(BPAN-LOCAL):
	git clone --depth=1 $(BPAN-REPO-URL) $@

$(YSJ-BIN): $(ROOT)/util/ysj
	mkdir -p $(dir $@)
	printf '%s\n' \
	  '#!/usr/bin/env bash' \
	  'exec $(abspath $<) "$$@"' > $@
	chmod 755 $@

$(YSJ-API-BIN): $(YSJ-BIN)
	ln -fs ys $@

$(YSJ-DOCKER-CONFIG):
	mkdir -p $@

ifeq ($(YAMLSCRIPT_ENGINE),graalvm)
ifndef PROPER-BUILD-ENV
$(CLI-BIN-GRAALVM): $(DOCKER-RUN-FILE)
	$(DOCKER-EXEC) make -C ys $@
else

$(CLI-BIN-GRAALVM): $(CLI-JAR) $(REFLECTION-JSON)
ifneq (true,$(LIBZ))
	$(error *** \
	  The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	# The next command may take a long time (a minute or so)
	( set -x && \
	  $(TIME) \
	  $(NATIVE-ENV) \
	  $(NATIVE-IMAGE) \
	    -H:ConfigurationFileDirectories=config/ \
	    $(NATIVE-OPTS) \
	    -jar $< \
	    -o $(CLI-OUT) \
	) 2>&1 | tee $(YS-BUILD-LOG)
	test -f $@
	touch $@
endif

else ifeq ($(YAMLSCRIPT_ENGINE),glojure)

GLOAT-CLI-SRCS := \
  $(GLOJURE-SRCS) \
  src/yamlscript/util/install.clj \
  src/yamlscript/util/compile.clj \
  gloat/src/yamlscript/util_platform.clj \
  gloat/src/yamlscript/cli.clj

$(CLI-BIN-GLOJURE): \
  $(GLOAT) $(GLOJURE-PREPARED-STAMP) $(GLOAT-CLI-SRCS)
	mkdir -p $(dir $@)
	</dev/null env -u GOROOT \
	  GLOAT_YS_RUNTIME=all \
	  GLOAT_MODULE=github.com/yaml/yamlscript \
	  GLOAT_EXTRA_GO_DIR=$(GO-YAML-GENERATED-DIR) \
	  GLOAT_ENGINE=$(GLOAT-ENGINE) \
	  $(GLOAT-RUN) --force $(GLOAT-PLATFORM-OPT) \
	  $(GLOAT-CLI-SRCS) -o $@
	test -x $@
ifeq (,$(GLOAT-WINDOWS-TARGET)$(GLOAT-WASI-TARGET))
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%)
endif

else
$(error Unknown YAMLSCRIPT_ENGINE '$(YAMLSCRIPT_ENGINE)')
endif

$(CLI-JAR): $(LEIN) $(CLI-JAR-DEPS)
	lein uberjar

$(CORE-INSTALLED): $(CORE-JAR)
	$(MAKE) -C $(CORE-DIR) $@

$(CORE-JAR):
	$(MAKE) -C $(CORE-DIR)
