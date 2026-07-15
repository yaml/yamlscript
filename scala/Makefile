include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/java.mk

include $(MAKES)/shell.mk

export COURSIER_CACHE := $(ROOT)/.cache/coursier
export JAVA_TOOL_OPTIONS := -Djna.tmpdir=$(ROOT)/.cache/jna

SCALA-CLI ?= scala-cli
SCALA-YAMLSCRIPT-JAR := target/scala-yamlscript-$(YAMLSCRIPT_VERSION).jar


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
	$(MVN) deploy:deploy-file \
	  -s ~/.mvn-user-settings.xml \
	  -Dfile=$(SCALA-YAMLSCRIPT-JAR) \
	  -DpomFile=pom.xml \
	  -DrepositoryId=clojars \
	  -Durl=https://clojars.org/repo/

clean::
	$(RM) -r .bsp .scala-build target
