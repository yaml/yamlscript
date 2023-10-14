include ../.common.mk
include ../.common-clj.mk

YAMLSCRIPT_CLI_BIN := ./ys
YAMLSCRIPT_CLI_SRC := src/yamlscript/cli.clj
YAMLSCRIPT_CLI_JAR_PATH := \
  target/uberjar/yamlscript.cli-0.1.0-SNAPSHOT-standalone.jar

#------------------------------------------------------------------------------
build:: $(YAMLSCRIPT_CLI_BIN)

jar: $(YAMLSCRIPT_CLI_JAR_PATH)

test:
	./test.sh

$(YAMLSCRIPT_CLI_BIN): $(YAMLSCRIPT_CLI_JAR_PATH) $(GRAALVM_PATH)
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
	$(RM) -r reports/ target/ .lein-* .cpcache/
	$(RM) $(YAMLSCRIPT_CLI_BIN)

distclean::
	$(RM) -r .calva/ .clj-kondo/ .lsp/

$(YAMLSCRIPT_CLI_JAR_PATH): $(YAMLSCRIPT_CORE_INSTALLED) $(YAMLSCRIPT_CLI_SRC)
	lein uberjar
