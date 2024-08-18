include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk
include ./rapidyaml.mk

RAPIDYAML_JAR_DEPS := \
  $(wildcard \
      $(JAVA_INSTALLED) \
      Makefile \
      pom.xml \
  )

RAPIDYAML_CLASS := src/main/java/org/rapidyaml/Rapidyaml.class


#------------------------------------------------------------------------------
default::
	echo $(RAPIDYAML_SO)

build:: $(RAPIDYAML_SO)
	@:

jar: $(RAPIDYAML_JAR)

install:: $(RAPIDYAML_INSTALLED)

test:: build $(JAVA_INSTALLED)
	mvn $@

test-x: build $(JAVA_INSTALLED)
	mvn -X -e test

clean::
	$(RM) $(RAPIDYAML_CLASS) $(RAPIDYAML_SO)
	$(RM) -r reports/ target/
	$(MAKE) -C native $@

realclean:: clean
	$(MAKE) -C native $@

sysclean::
	$(RM) -r $(MAVEN_REPOSITORY)/org/rapidyaml


#------------------------------------------------------------------------------
$(RAPIDYAML_SO): $(JAVA_INSTALLED)
	$(MAKE) -C native $@

$(RAPIDYAML_INSTALLED): $(RAPIDYAML_JAR) $(JAVA_INSTALLED)
	mvn install

$(RAPIDYAML_JAR): $(RAPIDYAML_CLASS) $(RAPIDYAML_JAR_DEPS)
	jar -cvf $@ $<

$(RAPIDYAML_CLASS): $(RAPIDYAML_JAVA) $(JAVA_INSTALLED)
	javac $<
