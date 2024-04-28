include ./rapidyaml.mk
include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk

RAPIDYAML_VERSION := 0.6.0

RAPIDYAML_JAVA_JAR := target/rapidyaml-$(RAPIDYAML_VERSION).jar

LIBRARY := native/librapidyaml.$(SO).$(RAPIDYAML_VERSION)

export LD_LIBRARY_PATH := \
  $(ROOT)/rapidyaml/native


#------------------------------------------------------------------------------

build:: $(LIBRARY) package

$(LIBRARY): $(wildcard native/*pp native/CMakeLists.txt native/Makefile)
	$(MAKE) -C native build
	cp -fav native/rapidyaml-build/librapidyaml.$(SO) $(LIBRARY)

test:: build

$(MVN_COMMANDS):: $(JAVA_INSTALLED) $(LIBRAPIDYAML_SO_FQNP)
	mvn $@

release: $(JAVA_INSTALLED) $(LIBRAPIDYAML_SO_FQNP) test package
ifndef n
	$(error 'make $@' needs the n variable set to the new version)
endif
	mvn deploy:deploy-file \
	    -s ~/.mvn-user-settings.xml \
	    -Dfile=$(RAPIDYAML_JAVA_JAR) \
	    -DpomFile=pom.xml \
	    -DrepositoryId=clojars \
	    -Durl=https://clojars.org/repo/

clean::
	$(MAKE) -C native $@
	$(RM) -r reports/ target/
	$(RM) -r $(LIBRARY)

realclean::
	$(MAKE) -C native $@
