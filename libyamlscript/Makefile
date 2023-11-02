include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/docker.mk

LIBYAMLSCRIPT_JAR_PATH := target/libyamlscript-0.1.0-standalone.jar

BUILD_TARGETS := \
  $(LIBYAMLSCRIPT_SO_PATH) \

# Avoid rebuild in ephemerally created Docker container.
ifdef DOCKERENV
ifneq (,$(wildcard $(LIBYAMLSCRIPT_SO_PATH)))
LIBYAMLSCRIPT_JAR_PATH :=
endif
endif

#------------------------------------------------------------------------------
default::

build:: $(BUILD_TARGETS)

install:: $(LIBYAMLSCRIPT_SO_PATH)
	install -m 644 $< $(prefix)/lib/

clean::
	$(RM) -r lib/
	$(RM) src/libyamlscript/Core.class

test:: $(LIBYAMLSCRIPT_SO_PATH)
	ls -lh lib

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

ifeq (true,$(IS_ROOT))
$(LIBYAMLSCRIPT_JAR_PATH):
else
$(LIBYAMLSCRIPT_JAR_PATH): $(YAMLSCRIPT_CORE_INSTALLED) $(GRAALVM_PATH)
	lein uberjar
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
