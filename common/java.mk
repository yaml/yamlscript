#------------------------------------------------------------------------------
# Set Java specific variables:
#------------------------------------------------------------------------------

JAVA-HOME := $(GRAALVM-HOME)
export JAVA_HOME := $(JAVA-HOME)
export PATH := $(JAVA-HOME)/bin:$(PATH)

YAMLSCRIPT-JAVA-INSTALLED := \
  $(MAVEN-REPOSITORY)/org/yamlscript/yamlscript/maven-metadata-local.xml

YAMLSCRIPT-JAVA-SRC := \
  $(ROOT)/java/src/main/java/org/yamlscript/yamlscript/*.java \


#------------------------------------------------------------------------------
java-home:
	@echo $(JAVA-HOME)

$(GRAALVM-HOME): $(GRAALVM-INSTALLED)

$(GRAALVM-INSTALLED): $(GRAALVM-DOWNLOAD)
	tar xzf $<
	mv graalvm-* $(GRAALVM-PATH)
	touch $@

$(GRAALVM-DOWNLOAD):
	$(call need-curl)
	$(CURL) -o $@ $(GRAALVM-URL)

$(YAMLSCRIPT-JAVA-INSTALLED): $(YAMLSCRIPT-JAVA-SRC)
	$(MAKE) -C $(ROOT)/java install
