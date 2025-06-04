CORE-DIR := $(ROOT)/core
CORE-JAR := $(CORE-DIR)/target/core-$(YAMLSCRIPT_VERSION)-standalone.jar
CORE-DEPS := $(shell find $(CORE-DIR)/src -name '*.clj')

CORE-INSTALLED := \
  $(MAVEN-REPOSITORY)/yamlscript/core/$(YAMLSCRIPT_VERSION)
CORE-INSTALLED := \
  $(CORE-INSTALLED)/core-$(YAMLSCRIPT_VERSION).jar
