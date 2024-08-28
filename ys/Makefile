include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/docker.mk
include $(COMMON)/native.mk

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

test := test/

YS_BUILD_DEPS := \
  $(YAMLSCRIPT_CLI_BIN) \
  $(YAMLSCRIPT_CLI_BIN_BASH) \

ifeq (true,$(IS_LINUX))
ifeq (true,$(IS_INTEL))
YS_BUILD_DEPS := $(MUSL_GCC) $(YS_BUILD_DEPS)
NATIVE_OPTS += \
  -H:CCompilerOption=-Wl,-z,stack-size=2097152 \
  --static \
  --libc=musl
endif
endif


#------------------------------------------------------------------------------
build:: $(YS_BUILD_DEPS)

install: build
	mkdir -p $(PREFIX)/bin
	install -m 755 $(YAMLSCRIPT_CLI_BIN) $(PREFIX)/bin/
	ln -fs $(notdir $(YAMLSCRIPT_CLI_BIN)) \
	  $(PREFIX)/$(YAMLSCRIPT_CLI_BIN:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $(YAMLSCRIPT_CLI_BIN)) \
	  $(PREFIX)/$(YAMLSCRIPT_CLI_BIN:%-$(YS_VERSION)=%)
	install -m 755 $(YAMLSCRIPT_CLI_BIN_BASH) \
	  $(PREFIX)/bin/

jar: $(YAMLSCRIPT_CLI_JAR_PATH) $(RAPIDYAML_INSTALLED)
	@:

test: test-unit

test-all: test-unit test-run

test-unit: $(LEIN) $(YAMLSCRIPT_CORE_INSTALLED)
	$< test

test-run: $(BPAN_LOCAL) build
	prove $${TEST_VERBOSE:+'-v'} $(test)

$(BPAN_LOCAL):
	git clone --depth=1 $(BPAN_REPO_URL) $@

$(YAMLSCRIPT_CLI_BIN): $(YAMLSCRIPT_CLI_JAR_PATH) $(RAPIDYAML_INSTALLED)
ifndef YS_BIN_DIR
ifneq (true,$(LIBZ))
	$(error *** \
	  The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	$(TIME) \
	native-image \
	  -H:ConfigurationFileDirectories=config/ \
	  $(NATIVE_OPTS) \
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

$(RAPIDYAML_INSTALLED):
	$(MAKE) -C $(RAPIDYAML) $@

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
