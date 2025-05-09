CORE_DIR := $(ROOT)/core
CORE_JAR := $(CORE_DIR)/target/core-$(YAMLSCRIPT_VERSION)-standalone.jar
CORE_DEPS := $(LEIN) $(shell find $(CORE_DIR)/src -name '*.clj')

CORE_INSTALLED := \
  $(MAVEN_REPOSITORY)/yamlscript/core/$(YAMLSCRIPT_VERSION)
CORE_INSTALLED := \
  $(CORE_INSTALLED)/core-$(YAMLSCRIPT_VERSION).jar
