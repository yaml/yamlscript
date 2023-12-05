include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/docker.mk

YAMLSCRIPT_CLI_BIN := bin/ys-$(YS_VERSION)
YAMLSCRIPT_CLI_SRC := \
  src/yamlscript/cli.clj \

YAMLSCRIPT_CLI_JAR_PATH := \
  target/uberjar/yamlscript.cli-$(YS_VERSION)-SNAPSHOT-standalone.jar

# Avoid rebuild in ephemerally created Docker container.
ifdef DOCKERENV
ifneq (,$(wildcard $(YAMLSCRIPT_CLI_BIN)))
YAMLSCRIPT_CLI_JAR_PATH :=
endif
endif

#------------------------------------------------------------------------------
build:: $(YAMLSCRIPT_CLI_BIN)

install: $(YAMLSCRIPT_CLI_BIN)
	mkdir -p $(PREFIX)/bin
	install -m 755 $< $(PREFIX)/bin/
	ln -fs $(notdir $<) $(PREFIX)/$(YAMLSCRIPT_CLI_BIN:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $<) $(PREFIX)/$(YAMLSCRIPT_CLI_BIN:%-$(YS_VERSION)=%)

jar: $(YAMLSCRIPT_CLI_JAR_PATH)
	@:

test: test-unit

test-all: test-unit test-ys

test-unit: $(LEIN)
	$< test

test-ys: $(YAMLSCRIPT_CLI_BIN)
	./test/test.sh
	@echo
	@echo "*** test.sh completed successfully"

$(YAMLSCRIPT_CLI_BIN): $(YAMLSCRIPT_CLI_JAR_PATH)
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
	\
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

realclean::
	$(RM) -r bin

ifeq (true,$(IS_ROOT))
$(YAMLSCRIPT_CLI_JAR_PATH):
else
$(YAMLSCRIPT_CLI_JAR_PATH): $(LEIN) $(YAMLSCRIPT_LANG_INSTALLED) $(YAMLSCRIPT_CLI_SRC)
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
