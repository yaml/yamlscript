LIBYS-DIR := $(ROOT)/libyamlscript
LIBYS-LIB := $(LIBYS-DIR)/lib

export LD_LIBRARY_PATH := $(LIBYS-LIB):$(LD_LIBRARY_PATH)
export $(DY)LD_LIBRARY_PATH := $(LD_LIBRARY_PATH)

LIBYS-SO-NAME := $(LIBYS-LIB)/libyamlscript
LIBYS-SO-FQNP := $(LIBYS-SO-NAME).$(SO).$(YAMLSCRIPT_VERSION)
LIBYS-SO-BASE := $(LIBYS-LIB)/libyamlscript.$(SO)
LIBYS-SO-APIP := $(LIBYS-SO-BASE).$(API_VERSION)
LIBYS-SO-VERS := $(LIBYS-LIB)/libyamlscript.$(YAMLSCRIPT_VERSION).$(SO)

LIBYS-DEPS := \
  $(LIBYS-SO-FQNP) \

LIBYS-JAR := \
  $(LIBYS-LIB)/target/libyamlscript-$(YAMLSCRIPT_VERSION)-standalone.jar

LIBYS-INSTALLED := \
  $(MAVEN-REPOSITORY)/org/yamlscript/yamlscript/$(YAMLSCRIPT_VERSION)
LIBYS-INSTALLED := \
  $(LIBYS-INSTALLED)/yamlscript-$(YAMLSCRIPT_VERSION).jar

LIBYS-JAR-PATH := \
  target/libyamlscript-$(YAMLSCRIPT_VERSION)-standalone.jar

LIBYS-SOURCES := \
  src/libyamlscript/core.clj \
  src/libyamlscript/API.java \

LIBYS-HEADERS := \
  $(LIBYS-LIB)/graal_isolate.h \
  $(LIBYS-SO-NAME).$(YAMLSCRIPT_VERSION).h \
