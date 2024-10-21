include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/docker.mk
include $(COMMON)/native.mk

LIBYAMLSCRIPT_JAR_PATH := target/libyamlscript-$(YS_VERSION)-standalone.jar

LYS_SOURCES := \
  src/libyamlscript/core.clj \
	src/libyamlscript/API.java \

HEADERS := \
  $(LIBYAMLSCRIPT_DIR)/graal_isolate.h \
  $(LIBYAMLSCRIPT_SO_NAME).$(YS_VERSION).h \

BUILD_TARGETS := \
  $(LIBYAMLSCRIPT_SO_FQNP) \
  $(HEADERS) \

# Avoid rebuild in ephemerally created Docker container.
ifdef DOCKERENV
ifneq (,$(wildcard $(LIBYAMLSCRIPT_SO_FQNP)))
LIBYAMLSCRIPT_JAR_PATH :=
endif
endif

#------------------------------------------------------------------------------
build:: $(BUILD_TARGETS)

jar: $(LIBYAMLSCRIPT_JAR_PATH)

install:: $(BUILD_TARGETS)
	mkdir -p $(PREFIX)/include/
	install -m 644 $(HEADERS) $(PREFIX)/include/
	mkdir -p $(PREFIX)/lib/
	install -m 644 $< $(PREFIX)/lib/
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYAMLSCRIPT_SO_BASE))
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYAMLSCRIPT_SO_APIP))
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYAMLSCRIPT_SO_VERS))

realclean::
	$(RM) -r lib/
	$(RM) src/libyamlscript/Core.class

test:: $(LIBYAMLSCRIPT_SO_FQNP)
	ls -lh lib

repl-deps:: $(LIBYAMLSCRIPT_JAR_PATH)

$(LIBYAMLSCRIPT_SO_FQNP): $(LIBYAMLSCRIPT_JAR_PATH)
ifneq (true,$(LIBZ))
	$(error *** The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	# The next command may take a long time (a minute or so)
	$(TIME) \
	native-image \
	  $(NATIVE_OPTS) \
	\
	  --shared \
	  -jar $< \
	  -o $(LIBYAMLSCRIPT_SO_NAME)
	mv $(LIBYAMLSCRIPT_SO_BASE) $@
	mv $(LIBYAMLSCRIPT_SO_NAME).h $(LIBYAMLSCRIPT_SO_NAME).$(YS_VERSION).h
	ln -fs $(notdir $@) $(LIBYAMLSCRIPT_SO_APIP)
	ln -fs $(notdir $@) $(LIBYAMLSCRIPT_SO_VERS)
	ln -fs $(notdir $@) $(LIBYAMLSCRIPT_SO_BASE)

ifeq (true,$(IS_ROOT))
$(LIBYAMLSCRIPT_JAR_PATH):
ifeq (true,$(IS_ROOT))
	$(error *** YAMLScript 'make build' must be run as non-root user)
endif
else
$(LIBYAMLSCRIPT_JAR_PATH): $(LEIN) $(JAVA_INSTALLED) $(YAMLSCRIPT_CORE_INSTALLED) $(LYS_SOURCES)
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
