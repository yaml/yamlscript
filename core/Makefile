include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/vars-core.mk

export PATH := $(ROOT)/core/bin:$(PATH)

#------------------------------------------------------------------------------
build::

test:: $(CORE_DEPS)
	$(LEIN) $@

install: $(CORE_INSTALLED)

$(CORE_INSTALLED): $(CORE_JAR)
	$(LEIN) install
	touch $@

$(CORE_JAR): $(CORE_DEPS)
	$(LEIN) uberjar
	touch $@

clean::
	$(RM) pom.xml
	$(RM) -r target/
