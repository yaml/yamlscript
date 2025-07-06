include $(MAKES)/graalvm.mk

YAMLSCRIPT-JAVA-INSTALLED := \
  $(MAVEN-REPOSITORY)/org/yamlscript/yamlscript/maven-metadata-local.xml

YAMLSCRIPT-JAVA-SRC := \
  $(ROOT)/java/src/main/java/org/yamlscript/yamlscript/*.java \


#------------------------------------------------------------------------------
$(YAMLSCRIPT-JAVA-INSTALLED): $(YAMLSCRIPT-JAVA-SRC)
	$(MAKE) -C $(ROOT)/java install
