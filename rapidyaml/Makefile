include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk

RAPIDYAML_JAR_DEPS := \
  $(wildcard \
      build \
      $(JAVA_INSTALLED) \
      Makefile \
      pom.xml \
  )

RAPIDYAML_CLASS := $(RAPIDYAML_JAVA:.java=.class)


#------------------------------------------------------------------------------
default::
	echo $(RAPIDYAML_SO)
	echo $(RAPIDYAML_LIB)

x:
	YS -ce 'foo: var'

build:: $(RAPIDYAML_JNI_H) $(RAPIDYAML_SO) $(RAPIDYAML_LIB) $(RAPIDYAML_JAR)
	@:

jar: $(RAPIDYAML_JAR)

install:: $(RAPIDYAML_INSTALLED)

test:: build $(JAVA_INSTALLED)
	$(MAKE) -C native $@
	$(MVN) $@

test-x: build $(JAVA_INSTALLED)
	$(MAKE) -C native $@
	$(MVN) -X -e test

clean::
	$(RM) $(RAPIDYAML_CLASS) $(RAPIDYAML_SO) $(RAPIDYAML_LIB)
	$(RM) -r reports/ target/
	$(MAKE) -C native $@

realclean:: clean
	$(MAKE) -C native $@
	$(RM) -r $(HOME)/.m2/repository/org/rapidyaml/

sysclean::
	$(RM) -r $(MAVEN_REPOSITORY)/org/rapidyaml

jni: $(RAPIDYAML_JNI_H)


#------------------------------------------------------------------------------
$(RAPIDYAML_SO): $(RAPIDYAML_JNI_H)
	$(MAKE) -C native $@
$(RAPIDYAML_LIB): $(RAPIDYAML_JNI_H)
	$(MAKE) -C native $@

$(RAPIDYAML_INSTALLED): $(RAPIDYAML_JAR) $(JAVA_INSTALLED)
	$(MVN) install

$(RAPIDYAML_JAR): $(RAPIDYAML_CLASS) $(JAVA_INSTALLED)
	$(JAR) -cvf $@ $<

$(RAPIDYAML_CLASS): $(RAPIDYAML_JAVA) $(JAVA_INSTALLED)
	@# this doesn't work:
	@#$(JAVAC) $<
	@# ... but this does:
	$(JAVAC) $(RAPIDYAML_JAVA)

$(RAPIDYAML_JNI_H): $(RAPIDYAML_JAVA)
	$(MAKE) -C native $@
