CORE-DIR := $(ROOT)/core
CORE-DIR-MAKE := $(ROOT-MAKE)/core
CORE-JAR := $(CORE-DIR-MAKE)/target/core-$(YAMLSCRIPT_VERSION)-standalone.jar
CORE-DEPS := $(shell find $(CORE-DIR)/src -name '*.clj')
CORE-DEPS := $(CORE-DEPS:$(ROOT)/%=$(ROOT-MAKE)/%)

CORE-INSTALLED := \
  $(MAVEN-REPOSITORY)/yamlscript/core/$(YAMLSCRIPT_VERSION)
CORE-INSTALLED := \
  $(CORE-INSTALLED)/core-$(YAMLSCRIPT_VERSION).jar
