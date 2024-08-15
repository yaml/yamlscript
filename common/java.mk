#------------------------------------------------------------------------------
# Set Java specific variables:
#------------------------------------------------------------------------------

export JAVA_HOME := $(GRAALVM_HOME)
export PATH := $(JAVA_HOME)/bin:$(PATH)

YAMLSCRIPT_JAVA_INSTALLED := \
  $(MAVEN_REPOSITORY)/org/yamlscript/yamlscript/maven-metadata-local.xml

YAMLSCRIPT_JAVA_SRC := \
  $(ROOT)/java/src/main/java/org/yamlscript/yamlscript/*.java \

RAPIDYAML_VERSION := 0.7.0
RAPIDYAML_JAR := $(ROOT)/rapidyaml/target/rapidyaml-$(RAPIDYAML_VERSION).jar
#RAPIDYAML_SO := $(ROOT)/rapidyaml/native/librapidyaml.$(SO).$(RAPIDYAML_VERSION)
RAPIDYAML_SO := $(ROOT)/rapidyaml/native/librapidyaml.$(SO)
RAPIDYAML_INSTALLED := \
  $(MAVEN_REPOSITORY)/org/rapidyaml/rapidyaml/$(RAPIDYAML_VERSION)/rapidyaml-$(RAPIDYAML_VERSION).jar


#------------------------------------------------------------------------------
$(YAMLSCRIPT_JAVA_INSTALLED): $(YAMLSCRIPT_JAVA_SRC)
	$(MAKE) -C $(ROOT)/java install
