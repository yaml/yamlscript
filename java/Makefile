include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/java.mk
include $(MAKES)/shell.mk

YAMLSCRIPT-JAVA-JAR := target/yamlscript-0.1.97.jar

MVN-COMMANDS := \
  compile \
  install \
  package \
  test \
  validate \

#------------------------------------------------------------------------------

$(MVN-COMMANDS):: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP)
	mvn $@

release: $(JAVA-INSTALLED) $(LIBYS-SO-FQNP) test package
ifndef n
	$(error 'make $@' needs the n variable set to the new version)
endif
	mvn deploy:deploy-file \
	    -s ~/.mvn-user-settings.xml \
	    -Dfile=$(YAMLSCRIPT-JAVA-JAR) \
	    -DpomFile=pom.xml \
	    -DrepositoryId=clojars \
	    -Durl=https://clojars.org/repo/

clean::
	$(RM) -r reports/ target/
