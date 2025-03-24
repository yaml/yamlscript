include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/vars-core.mk
include $(COMMON)/vars-rapidyaml.mk

export PATH := $(ROOT)/core/bin:$(PATH)

#------------------------------------------------------------------------------

test:: $(LEIN) $(CORE-DEPS)
	lein $@

install: $(CORE-INSTALLED)

repl-deps:: $(RAPIDYAML-INSTALLED)

$(CORE-INSTALLED): $(LEIN) $(CORE-DEPS)
	lein install
	touch $@

$(CORE-JAR): $(LEIN) $(CORE-DEPS)
	lein uberjar

$(RAPIDYAML_INSTALLED):
	$(MAKE) -C $(RAPIDYAML_DIR) $@

clean::
	$(RM) pom.xml
	$(RM) -r target/
