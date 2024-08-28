#------------------------------------------------------------------------------
# Set Java specific variables:
#------------------------------------------------------------------------------

export JAVA_HOME := $(GRAALVM_HOME)
export PATH := $(JAVA_HOME)/bin:$(PATH)

YAMLSCRIPT_JAVA_INSTALLED := \
  $(MAVEN_REPOSITORY)/org/yamlscript/yamlscript/maven-metadata-local.xml

YAMLSCRIPT_JAVA_SRC := \
  $(ROOT)/java/src/main/java/org/yamlscript/yamlscript/*.java \


#------------------------------------------------------------------------------
java-home:
	@echo $(JAVA_HOME)

$(GRAALVM_HOME): $(GRAALVM_INSTALLED)

$(GRAALVM_INSTALLED): $(GRAALVM_DOWNLOAD)
	tar xzf $<
	mv graalvm-* $(GRAALVM_PATH)
	touch $@

$(GRAALVM_DOWNLOAD):
ifeq (,$(CURL))
	$(error *** 'curl' is required but not installed)
endif
	$(CURL) -L -o $@ $(GRAALVM_URL)

$(YAMLSCRIPT_JAVA_INSTALLED): $(YAMLSCRIPT_JAVA_SRC)
	$(MAKE) -C $(ROOT)/java install
