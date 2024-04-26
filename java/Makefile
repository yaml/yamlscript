include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/java.mk

YAMLSCRIPT_JAVA_JAR := target/yamlscript-0.1.58.jar

#------------------------------------------------------------------------------

$(MVN_COMMANDS):: $(JAVA_INSTALLED) $(LIBYAMLSCRIPT_SO_FQNP)
	mvn $@

release: $(JAVA_INSTALLED) $(LIBYAMLSCRIPT_SO_FQNP) test package
ifndef n
	$(error 'make $@' needs the n variable set to the new version)
endif
	mvn deploy:deploy-file \
	    -s ~/.mvn-user-settings.xml \
	    -Dfile=$(YAMLSCRIPT_JAVA_JAR) \
	    -DpomFile=pom.xml \
	    -DrepositoryId=clojars \
	    -Durl=https://clojars.org/repo/

clean::
	$(RM) -r reports/ target/
