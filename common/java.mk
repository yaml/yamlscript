#------------------------------------------------------------------------------
# Set Java specific variables:
#------------------------------------------------------------------------------

MVN_COMMANDS := \
  deploy \
  install \
  package \
  test \

export JAVA_HOME := $(GRAALVM_HOME)
export PATH := $(JAVA_HOME)/bin:$(PATH)

YAMLSCRIPT_JAVA_INSTALLED := \
  $(MAVEN_REPOSITORY)/org/yaml/yamlscript-java/maven-metadata-local.xml

YAMLSCRIPT_JAVA_SRC := \
  $(ROOT)/java/src/main/java/org/yaml/yamlscript/*.java \


#------------------------------------------------------------------------------
$(YAMLSCRIPT_JAVA_INSTALLED): $(YAMLSCRIPT_JAVA_SRC)
	$(MAKE) -C $(ROOT)/java install
