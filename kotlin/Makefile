include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/java.mk

include $(MAKES)/shell.mk

export JAVA_TOOL_OPTIONS := -Djna.tmpdir=$(ROOT)/.cache/jna

KOTLIN-YAMLSCRIPT-JAR := target/kotlin-yamlscript-0.2.27.jar
MAVEN-CENTRAL-ARTIFACT := kotlin-yamlscript
MAVEN-CENTRAL-JAR := $(KOTLIN-YAMLSCRIPT-JAR)
MAVEN-CENTRAL-SOURCE-FILES := \
  $(wildcard src/main/kotlin/org/yamlscript/yamlscript/*.kt)
MAVEN-CENTRAL-JAVADOC-TEXT := \
  YAMLScript Kotlin API documentation is in the sources JAR.

include $(COMMON)/maven-central.mk

MVN-COMMANDS := \
  compile \
  install \
  package \
  validate \

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

$(MVN-COMMANDS):: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP)
	mkdir -p $(ROOT)/.cache/jna
	$(MVN) $@

# The Kotlin wrapper depends on the Java binding artifact, so install
# that into the local Maven repository first:
test:: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP)
	mkdir -p $(ROOT)/.cache/jna
	$(MAKE) -C $(ROOT)/java install
	$(MVN) test

ifdef YS_RELEASE_CI
release: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP)
else
release: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP) test package
endif
ifndef n
	$(error 'make $@' needs the n variable set to the new version)
endif
ifdef YS_RELEASE_CI
	$(MAKE) -C $(ROOT)/java install
	$(MVN) package -DskipTests
endif
	$(MAKE) maven-central-release

clean::
	$(RM) -r reports/ target/
