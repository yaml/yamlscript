include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/docker.mk

YAMLSCRIPT_CLI_BIN := bin/ys-$(YS_VERSION)
YAMLSCRIPT_CLI_SRC := \
  src/yamlscript/cli.clj \

YAMLSCRIPT_CLI_BIN_BASH := bin/ys-sh-$(YS_VERSION)
YAMLSCRIPT_CLI_BIN_BASH_SRC := share/ys-0.bash

YAMLSCRIPT_CLI_JAR_PATH := \
  target/uberjar/yamlscript.cli-$(YS_VERSION)-SNAPSHOT-standalone.jar

# Avoid rebuild in ephemerally created Docker container.
ifdef DOCKERENV
ifneq (,$(wildcard $(YAMLSCRIPT_CLI_BIN)))
YAMLSCRIPT_CLI_JAR_PATH :=
endif
endif

BPAN_LOCAL := .bpan
BPAN_REPO_URL := https://github.com/bpan-org/bpan

#------------------------------------------------------------------------------
build:: $(YAMLSCRIPT_CLI_BIN_BASH) $(YAMLSCRIPT_CLI_BIN)

install: build
	mkdir -p $(PREFIX)/bin
	install -m 755 $(YAMLSCRIPT_CLI_BIN) $(PREFIX)/bin/
	ln -fs $(notdir $(YAMLSCRIPT_CLI_BIN)) $(PREFIX)/$(YAMLSCRIPT_CLI_BIN:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $(YAMLSCRIPT_CLI_BIN)) $(PREFIX)/$(YAMLSCRIPT_CLI_BIN:%-$(YS_VERSION)=%)
	install -m 755 $(YAMLSCRIPT_CLI_BIN_BASH) $(PREFIX)/bin/

jar: $(YAMLSCRIPT_CLI_JAR_PATH)
	@:

test: test-unit

test-all: test-unit test-ys test-run

test-unit: $(LEIN) $(YAMLSCRIPT_CORE_INSTALLED)
	$< test

test-ys: $(YAMLSCRIPT_CLI_BIN)
	./test/test.sh
	@echo
	@echo "*** test.sh completed successfully"

test-run: $(BPAN_LOCAL) $(YAMLSCRIPT_CLI_BIN)
	prove $${TEST_VERBOSE:+'-v'} test/

$(BPAN_LOCAL):
	git clone --depth=1 $(BPAN_REPO_URL) $@

$(YAMLSCRIPT_CLI_BIN): $(YAMLSCRIPT_CLI_JAR_PATH)
ifndef YS_BIN_DIR
ifneq (true,$(LIBZ))
	$(error *** The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	$(TIME) \
	native-image \
	    -O$(GRAALVM_O) \
	    --verbose \
	    --native-image-info \
	    --no-fallback \
	\
	    --initialize-at-build-time \
	    --enable-preview \
	    --enable-url-protocols=https \
	\
	    -H:ReflectionConfigurationFiles=reflection.json \
	    -H:+ReportExceptionStackTraces \
	    -H:IncludeResources=SCI_VERSION \
	    -H:Log=registerResource: \
	    -J-Dclojure.spec.skip-macros=true \
	    -J-Dclojure.compiler.direct-linking=true \
	    -J-Xmx3g \
	\
	    -jar $< \
	    -o $@
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%)
endif

$(YAMLSCRIPT_CLI_BIN_BASH): $(YAMLSCRIPT_CLI_BIN_BASH_SRC)
	mkdir -p $(dir $@)
	cp $< $@
	chmod 755 $@

clean::
	$(RM) -r .cpcache/

realclean::
	$(RM) -r bin lib $(BPAN_LOCAL)

ifeq (true,$(IS_ROOT))
$(YAMLSCRIPT_CLI_JAR_PATH):
else
$(YAMLSCRIPT_CLI_JAR_PATH): $(LEIN) $(YAMLSCRIPT_CORE_INSTALLED) $(YAMLSCRIPT_CLI_SRC)
	$< uberjar
endif

Dockerfile:: $(COMMON) Makefile
	cat \
	  $</docker-from-ubuntu.dockerfile \
	  $</docker-apt-base.dockerfile \
	  $</docker-useradd.dockerfile \
	  $</docker-apt-clojure.dockerfile \
	  $</docker-install-graalvm.dockerfile \
	  $</docker-copy-project-deps.dockerfile \
	  $</docker-deps-clojure.dockerfile \
	  $</docker-apt-dev.dockerfile \
	  > $@
