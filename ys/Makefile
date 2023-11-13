include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/docker.mk

YAMLSCRIPT_CLI_BIN := ys
YAMLSCRIPT_CLI_SRC := src/yamlscript/cli.clj
YAMLSCRIPT_CLI_JAR_PATH := \
  target/uberjar/yamlscript.cli-0.1.0-SNAPSHOT-standalone.jar

# Avoid rebuild in ephemerally created Docker container.
ifdef DOCKERENV
ifneq (,$(wildcard $(YAMLSCRIPT_CLI_BIN)))
YAMLSCRIPT_CLI_JAR_PATH :=
endif
endif

#------------------------------------------------------------------------------
build:: $(YAMLSCRIPT_CLI_BIN)

install: build
	install -m 755 $(YAMLSCRIPT_CLI_BIN) $(PREFIX)/bin/

jar: $(YAMLSCRIPT_CLI_JAR_PATH)
	@:

test: $(YAMLSCRIPT_CLI_BIN)
	./test.sh

$(YAMLSCRIPT_CLI_BIN): $(YAMLSCRIPT_CLI_JAR_PATH)
ifneq (true,$(LIBZ))
	$(error *** The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	time \
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

clean::
	$(RM) $(YAMLSCRIPT_CLI_BIN)

distclean::
	$(RM) -r .calva/ .clj-kondo/ .lsp/

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
