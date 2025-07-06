include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/vars-core.mk

export PATH := $(ROOT)/core/bin:$(PATH)

#------------------------------------------------------------------------------
build::

test:: $(LEIN) $(CORE-DEPS)
	lein $@

install: $(CORE-INSTALLED)

$(CORE-INSTALLED): $(LEIN) $(CORE-DEPS)
	lein install
	touch $@

$(CORE-JAR): $(LEIN) $(CORE-DEPS)
	lein uberjar

clean::
	$(RM) pom.xml
	$(RM) -r target/
