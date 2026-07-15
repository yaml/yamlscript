include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/java.mk

include $(MAKES)/scala.mk
include $(MAKES)/shell.mk

export COURSIER_CACHE := $(ROOT)/.cache/coursier
export JAVA_TOOL_OPTIONS := -Djna.tmpdir=$(ROOT)/.cache/jna

SCALA-CLI ?= scala-cli
SCALA-YAMLSCRIPT-JAR := target/scala-yamlscript-$(YAMLSCRIPT_VERSION).jar
MAVEN-CENTRAL-ARTIFACT := scala-yamlscript
MAVEN-CENTRAL-JAR := $(SCALA-YAMLSCRIPT-JAR)
MAVEN-CENTRAL-SOURCE-FILES := $(wildcard src/*.scala)
MAVEN-CENTRAL-JAVADOC-TEXT := \
  YAMLScript Scala API documentation is in the sources JAR.

include $(COMMON)/maven-central.mk


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(SCALA-CLI)
	mkdir -p $(ROOT)/.cache/jna
	scala-cli run --server=false src test

package: $(JAVA-INSTALLED)
	$(MVN) package -DskipTests

release: $(JAVA-INSTALLED) package
ifndef n
	$(error 'make $@' needs the n variable set to the new version)
endif
	$(MAKE) maven-central-release

clean::
	$(RM) -r .bsp .scala-build src/.bsp src/.scala-build target
