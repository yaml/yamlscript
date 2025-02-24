include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk

RAPIDYAML_CLASS := $(RAPIDYAML_JAVA:.java=.class)
RAPIDYAML_CLASSES := \
  $(RAPIDYAML_CLASS:$(RAPIDYAML)/src/main/java/%=-C src/main/java %)


#------------------------------------------------------------------------------
default::

build:: $(RAPIDYAML_JNI_H) $(RAPIDYAML_SO) $(RAPIDYAML_LIB) $(RAPIDYAML_JAR)
	@:

jar: $(RAPIDYAML_JAR)

jar-test: $(RAPIDYAML_JAR)
	java -jar $<

install:: $(RAPIDYAML_INSTALLED)

test:: build $(JAVA_INSTALLED)
	$(MAKE) -C native $@
	YS_RAPIDYAML_MAVEN_TEST=1 $(MVN) $@

test-x: build $(JAVA_INSTALLED)
	$(MAKE) -C native test
	YS_RAPIDYAML_MAVEN_TEST=1 $(MVN) -X -e test

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

# XXX Probably don't need this:
$(RAPIDYAML_LIB): $(RAPIDYAML_JNI_H)
	$(MAKE) -C native $@

$(RAPIDYAML_INSTALLED): $(RAPIDYAML_JAR)
ifdef YS_MAVEN_TEST
	YS_RAPIDYAML_MAVEN_TEST=1 $(MVN) install
else
	$(MVN) install -Dmaven.test.skip
endif
	touch $@

$(RAPIDYAML_JAR): $(RAPIDYAML_CLASS) $(RAPIDYAML_SO)
	$(JAR) -cf $@ $(RAPIDYAML_CLASSES)
	$(JAR) -uf $@ -C $(RAPIDYAML)/native/ $(RAPIDYAML_SO_NAME)
	$(JAR) -umf manifest.txt $@

$(RAPIDYAML_CLASS): $(RAPIDYAML_JAVA) $(JAVA_INSTALLED)
	@# this doesn't work:
	@#$(JAVAC) $<
	@# ... but this does:
	$(JAVAC) $(RAPIDYAML_JAVA)

$(RAPIDYAML_JNI_H): $(RAPIDYAML_JAVA)
	$(MAKE) -C native $@
