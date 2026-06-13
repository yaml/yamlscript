include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/java.mk

include $(MAKES)/shell.mk

YAMLSCRIPT-JAVA-JAR := target/yamlscript-0.2.19.jar

MVN-COMMANDS := \
  compile \
  install \
  package \
  test \
  validate \

#------------------------------------------------------------------------------

$(MVN-COMMANDS):: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP)
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
	$(MVN) deploy:deploy-file \
	    -s ~/.mvn-user-settings.xml \
	    -Dfile=$(YAMLSCRIPT-JAVA-JAR) \
	    -DpomFile=pom.xml \
	    -DrepositoryId=clojars \
	    -Durl=https://clojars.org/repo/

clean::
	$(RM) -r reports/ target/
