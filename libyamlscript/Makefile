include ../.common.mk
include ../.common-clj.mk

LIBYAMLSCRIPT_JAR_PATH := target/libyamlscript-0.1.0-standalone.jar

BUILD_TARGETS := \
  $(LIBYAMLSCRIPT_SO_PATH) \

#------------------------------------------------------------------------------
build:: $(BUILD_TARGETS)

clean::
	$(RM) -r lib/ target/ .lein-* .cpcache/
	$(RM) src/libyamlscript/Core.class

repl-deps:: $(LIBYAMLSCRIPT_JAR_PATH)

distclean::

$(LIBYAMLSCRIPT_SO_PATH): $(LIBYAMLSCRIPT_JAR_PATH) $(GRAALVM_PATH)
	mkdir -p $(dir $@)
	# The next command may take a long time (a minute or so)
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
	    -H:ReflectionConfigurationFiles=reflection.json \
	    -H:+ReportExceptionStackTraces \
	    -H:IncludeResources=SCI_VERSION \
	    -H:Log=registerResource: \
	    -J-Dclojure.spec.skip-macros=true \
	    -J-Dclojure.compiler.direct-linking=true \
	    -J-Xmx3g \
	\
	    --shared \
	    -jar $(LIBYAMLSCRIPT_JAR_PATH) \
	    -H:Name=$(LIBYAMLSCRIPT_SO_NAME)

$(LIBYAMLSCRIPT_JAR_PATH): $(YAMLSCRIPT_CORE_INSTALLED) $(GRAALVM_PATH)
	lein uberjar
