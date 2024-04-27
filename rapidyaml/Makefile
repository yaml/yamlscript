include ../common/base.mk
include $(COMMON)/java.mk

RYML_VERSION := 0.5.0

RYML_JAVA_JAR := target/ryml-$(RYML_VERSION).jar

LIBRARY := native/rapidyaml-install/lib/libryml.$(SO).$(RYML_VERSION)


export LD_LIBRARY_PATH := \
  $(ROOT)/rapidyaml/native/rapidyaml-install/lib

#------------------------------------------------------------------------------

build:: $(LIBRARY) package

$(LIBRARY):
	$(MAKE) -C native build

test:: build

$(MVN_COMMANDS):: $(JAVA_INSTALLED) $(LIBRYML_SO_FQNP)
	mvn $@

release: $(JAVA_INSTALLED) $(LIBRYML_SO_FQNP) test package
ifndef n
	$(error 'make $@' needs the n variable set to the new version)
endif
	mvn deploy:deploy-file \
	    -s ~/.mvn-user-settings.xml \
	    -Dfile=$(RYML_JAVA_JAR) \
	    -DpomFile=pom.xml \
	    -DrepositoryId=clojars \
	    -Durl=https://clojars.org/repo/

clean::
	$(MAKE) -C native $@
	$(RM) -r reports/ target/

realclean::
	$(MAKE) -C native $@
