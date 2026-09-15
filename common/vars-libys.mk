LIBYS-DIR := $(ROOT)/libys
LIBYS-LIB := $(LIBYS-DIR)/lib

export LD_LIBRARY_PATH := $(LIBYS-LIB):$(LD_LIBRARY_PATH)
ifneq ($(OS-NAME),windows)
export $(DY)LD_LIBRARY_PATH := $(LD_LIBRARY_PATH)
endif

LIBYS-SO-NAME := $(LIBYS-LIB)/libys
LIBYS-SO-GLOJURE := $(LIBYS-SO-NAME)-glojure.$(SO)
LIBYS-SO-GRAALVM := $(LIBYS-SO-NAME)-graalvm.$(SO)
LIBYS-SO-ENGINE := $(if $(filter graalvm,$(YAMLSCRIPT_ENGINE)),\
  $(LIBYS-SO-GRAALVM),$(LIBYS-SO-GLOJURE))
ifeq ($(TARGET-OS),windows)
LIBYS-SO-FQNP := $(LIBYS-SO-NAME).$(SO)
LIBYS-SO-BASE := $(LIBYS-SO-FQNP)
LIBYS-SO-APIP := $(LIBYS-SO-FQNP)
LIBYS-SO-VERS := $(LIBYS-SO-FQNP)
else
LIBYS-SO-FQNP := $(LIBYS-SO-NAME).$(SO).$(YAMLSCRIPT_VERSION)
LIBYS-SO-BASE := $(LIBYS-LIB)/libys.$(SO)
LIBYS-SO-APIP := $(LIBYS-SO-BASE).$(API_VERSION)
LIBYS-SO-VERS := $(LIBYS-LIB)/libys.$(YAMLSCRIPT_VERSION).$(SO)
endif

LIBYS-DEPS := \
  $(LIBYS-SO-FQNP) \

LIBYS-JAR := \
  $(LIBYS-LIB)/target/libys-$(YAMLSCRIPT_VERSION)-standalone.jar

LIBYS-INSTALLED := \
  $(MAVEN-REPOSITORY)/org/yamlscript/yamlscript/$(YAMLSCRIPT_VERSION)
LIBYS-INSTALLED := \
  $(LIBYS-INSTALLED)/yamlscript-$(YAMLSCRIPT_VERSION).jar

LIBYS-JAR-PATH := \
  target/libys-$(YAMLSCRIPT_VERSION)-standalone.jar

LIBYS-SOURCES := \
  src/libys/core.clj \
  src/libys/API.java \

ifeq ($(TARGET-OS),windows)
LIBYS-API-HEADER := $(LIBYS-SO-NAME).h
else
LIBYS-API-HEADER := $(LIBYS-SO-NAME).$(YAMLSCRIPT_VERSION).h
endif

LIBYS-HEADERS := \
  $(LIBYS-LIB)/graal_isolate.h \
  $(LIBYS-API-HEADER) \

LIBYS-GLOJURE-HEADER := $(LIBYS-DIR)/gloat/include/libys.h
LIBYS-GLOJURE-ISOLATE-HEADER := \
  $(LIBYS-DIR)/gloat/include/graal_isolate.h

LIBYS-BUILD-LOG := $(ROOT)/build-libys.log
