include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/vars-core.mk
include $(COMMON)/vars-rapidyaml.mk

export PATH := $(ROOT)/core/bin:$(PATH)

#------------------------------------------------------------------------------

test:: $(CORE_DEPS)
	$(LEIN) $@

install: $(CORE_INSTALLED)

repl-deps:: $(RAPIDYAML_INSTALLED)

$(CORE_INSTALLED): $(CORE_JAR) $(RAPIDYAML_INSTALLED)
	$(LEIN) install
	touch $@

$(CORE_JAR): $(CORE_DEPS)
	$(LEIN) uberjar
	touch $@

$(RAPIDYAML_INSTALLED):
	$(MAKE) -C $(RAPIDYAML_DIR) $@

clean::
	$(RM) pom.xml
	$(RM) -r target/
