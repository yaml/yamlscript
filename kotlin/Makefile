include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/java.mk

include $(MAKES)/shell.mk

KOTLIN-YAMLSCRIPT-JAR := target/kotlin-yamlscript-0.2.27.jar

MVN-COMMANDS := \
  compile \
  install \
  package \
  validate \

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

$(MVN-COMMANDS):: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP)
	$(MVN) $@

# The Kotlin wrapper depends on the Java binding artifact, so install
# that into the local Maven repository first:
test:: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP)
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
	$(MVN) deploy:deploy-file \
	    -s ~/.mvn-user-settings.xml \
	    -Dfile=$(KOTLIN-YAMLSCRIPT-JAR) \
	    -DpomFile=pom.xml \
	    -DrepositoryId=clojars \
	    -Durl=https://clojars.org/repo/

clean::
	$(RM) -r reports/ target/
