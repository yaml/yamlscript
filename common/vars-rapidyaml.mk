RAPIDYAML_DIR := $(ROOT)/rapidyaml
NATIVE_DIR := $(RAPIDYAML_DIR)/native

export LD_LIBRARY_PATH := $(NATIVE_DIR):$(LD_LIBRARY_PATH)
export $(DY)LD_LIBRARY_PATH := $(LD_LIBRARY_PATH)

RAPIDYAML_VERSION := 0.8.0
RAPIDYAML_TAG ?= d3132a25ec21c65e27ff46ab8c3d61c72a321302
RAPIDYAML_REPO := https://github.com/biojppm/rapidyaml
RAPIDYAML_BUILD_TYPE ?= Release
RAPIDYAML_DBG ?= 0
RAPIDYAML_TIMED ?= 1
RAPIDYAML_JAVA := \
  $(RAPIDYAML_DIR)/src/main/java/org/rapidyaml/Rapidyaml.java \
  $(RAPIDYAML_DIR)/src/main/java/org/rapidyaml/NativeLibLoader.java \
  $(RAPIDYAML_DIR)/src/main/java/org/rapidyaml/Evt.java \
  $(RAPIDYAML_DIR)/src/main/java/org/rapidyaml/YamlParseErrorException.java
RAPIDYAML_JNI_H := $(NATIVE_DIR)/org_rapidyaml_Rapidyaml.h
RAPIDYAML_SO_NAME := librapidyaml.$(RAPIDYAML_VERSION).$(SO)
RAPIDYAML_SO := $(NATIVE_DIR)/$(RAPIDYAML_SO_NAME)
RAPIDYAML_LIB := $(NATIVE_DIR)/librapidyaml.$(DOTLIB)
RAPIDYAML_JAR := $(RAPIDYAML_DIR)/target/rapidyaml-$(RAPIDYAML_VERSION).jar
RAPIDYAML_INSTALLED := \
  $(MAVEN_REPOSITORY)/org/rapidyaml/rapidyaml/$(RAPIDYAML_VERSION)
RAPIDYAML_INSTALLED := \
  $(RAPIDYAML_INSTALLED)/rapidyaml-$(RAPIDYAML_VERSION).jar
