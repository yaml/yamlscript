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

build:: $(RAPIDYAML_JNI_H) $(RAPIDYAML_SO) $(RAPIDYAML_LIB)
	@:

jar: $(RAPIDYAML_JAR)

install:: $(RAPIDYAML_INSTALLED)

test:: build $(JAVA_INSTALLED)
	$(MAKE) -C native $@
	mvn $@

test-x: build $(JAVA_INSTALLED)
	$(MAKE) -C native $@
	mvn -X -e test

clean::
	$(RM) $(RAPIDYAML_CLASS) $(RAPIDYAML_SO) $(RAPIDYAML_LIB)
	$(RM) -r reports/ target/
	$(MAKE) -C native $@

realclean:: clean
	$(MAKE) -C native $@

sysclean::
	$(RM) -r $(MAVEN_REPOSITORY)/org/rapidyaml

jni: $(RAPIDYAML_JNI_H)


#------------------------------------------------------------------------------
$(RAPIDYAML_SO): $(RAPIDYAML_JNI_H)
	$(MAKE) -C native $@
$(RAPIDYAML_LIB): $(RAPIDYAML_JNI_H)
	$(MAKE) -C native $@

$(RAPIDYAML_INSTALLED): $(RAPIDYAML_JAR) $(JAVA_INSTALLED)
	mvn install

$(RAPIDYAML_JAR): $(RAPIDYAML_CLASS) $(JAVA_INSTALLED)
	jar -cvf $@ $<

$(RAPIDYAML_CLASS): $(RAPIDYAML_JAVA) $(JAVA_INSTALLED)
	@# this doesn't work:
	@#javac $<
	@# ... but this does:
	javac $(RAPIDYAML_JAVA)

$(RAPIDYAML_JNI_H): $(RAPIDYAML_JAVA)
	$(MAKE) -C native $@
