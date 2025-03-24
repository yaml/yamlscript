LIBYS_DIR := $(ROOT)/libyamlscript
LIBYS_LIB := $(LIBYS_DIR)/lib

export LD_LIBRARY_PATH := $(LIBYS_LIB):$(LD_LIBRARY_PATH)
export $(DY)LD_LIBRARY_PATH := $(LD_LIBRARY_PATH)

LIBYS_SO_NAME := $(LIBYS_LIB)/libyamlscript
LIBYS_SO_FQNP := $(LIBYS_SO_NAME).$(SO).$(YAMLSCRIPT_VERSION)
LIBYS_SO_BASE := $(LIBYS_LIB)/libyamlscript.$(SO)
LIBYS_SO_APIP := $(LIBYS_SO_BASE).$(API_VERSION)
LIBYS_SO_VERS := $(LIBYS_LIB)/libyamlscript.$(YAMLSCRIPT_VERSION).$(SO)

LIBYS_DEPS := \
  $(LIBYS_SO_FQNP) \

LIBYS_JAR := \
  $(LIBYS_LIB)/target/libyamlscript-$(YAMLSCRIPT_VERSION)-standalone.jar

LIBYS_INSTALLED := \
  $(MAVEN_REPOSITORY)/org/yamlscript/yamlscript/$(YAMLSCRIPT_VERSION)
LIBYS_INSTALLED := \
  $(LIBYS_INSTALLED)/yamlscript-$(YAMLSCRIPT_VERSION).jar

LIBYS_JAR_PATH := \
  target/libyamlscript-$(YAMLSCRIPT_VERSION)-standalone.jar

LIBYS_SOURCES := \
  src/libyamlscript/core.clj \
  src/libyamlscript/API.java \

LIBYS_HEADERS := \
  $(LIBYS_LIB)/graal_isolate.h \
  $(LIBYS_SO_NAME).$(YAMLSCRIPT_VERSION).h \
