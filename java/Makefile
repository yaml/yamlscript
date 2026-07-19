include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/java.mk

include $(MAKES)/shell.mk

export JAVA_TOOL_OPTIONS := -Djna.tmpdir=$(ROOT)/.cache/jna

YAMLSCRIPT-JAVA-JAR := target/yamlscript-0.2.29.jar
MAVEN-CENTRAL-ARTIFACT := yamlscript
MAVEN-CENTRAL-JAR := $(YAMLSCRIPT-JAVA-JAR)
MAVEN-CENTRAL-SOURCE-FILES := \
  $(wildcard src/main/java/org/yamlscript/yamlscript/*.java)
MAVEN-CENTRAL-JAVADOC-TEXT := \
  YAMLScript Java API documentation is in the sources JAR.

include $(COMMON)/maven-central.mk

MVN-COMMANDS := \
  compile \
  install \
  package \
  test \
  validate \

#------------------------------------------------------------------------------

$(MVN-COMMANDS):: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP)
	mkdir -p $(ROOT)/.cache/jna
	$(MVN) $@

ifdef YS_RELEASE_CI
release: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP)
else
release: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP) test package
endif
ifndef n
	$(error 'make $@' needs the n variable set to the new version)
endif
ifdef YS_RELEASE_CI
	$(MVN) package -DskipTests
endif
	$(MAKE) maven-central-release

clean::
	$(RM) -r reports/ target/
