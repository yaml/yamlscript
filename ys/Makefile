include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/native.mk
include $(COMMON)/vars-cli.mk

BPAN-LOCAL := .bpan
BPAN-REPO-URL := https://github.com/bpan-org/bpan

test := test/


#------------------------------------------------------------------------------
build:: $(CLI-DEPS)

install: $(CLI-DEPS)
	mkdir -p $(PREFIX)/bin
	install -m 755 $(CLI-BIN) $(PREFIX)/bin/
	ln -fs $(notdir $(CLI-BIN)) \
	  $(PREFIX)/$(CLI-BIN:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $(CLI-BIN)) \
	  $(PREFIX)/$(CLI-BIN:%-$(YS_VERSION)=%)
	install -m 755 $(CLI-BIN-BASH) \
	  $(PREFIX)/bin/

jar: $(CLI-JAR)

test: test-run

test-all: test-unit test-run

test-unit: $(LEIN) $(CORE-INSTALLED)
	$< test $(lein-test)

test-run: $(CLI-DEPS) $(BPAN-LOCAL)
	prove $${TEST_VERBOSE:+'-v'} $(test)

repl-deps:: $(CORE-INSTALLED)

clean::
	$(RM) -r .cpcache/

realclean::
	$(RM) -r bin lib $(BPAN-LOCAL)

$(BPAN-LOCAL):
	git clone --depth=1 $(BPAN-REPO-URL) $@

$(CLI-BIN): $(CLI-JAR) $(REFLECTION-JSON)
ifneq (true,$(LIBZ))
	$(error *** \
	  The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	$(TIME) \
	native-image \
	  -H:ConfigurationFileDirectories=config/ \
	  $(NATIVE-OPTS) \
	\
	  -jar $< \
	  -o $@
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%)
	touch $@

$(CLI-BIN-BASH): $(CLI-BIN-BASH-SRC)
	mkdir -p $(dir $@)
	cp $< $@
	chmod 755 $@

$(CLI-JAR): $(LEIN) $(CLI-JAR-DEPS)
	$< uberjar

$(CORE-INSTALLED): $(CORE-JAR)
	@$(MAKE) -C $(CORE-DIR) $@ > /dev/null 2>&1

$(CORE-JAR):
	@$(MAKE) -C $(CORE-DIR) >/dev/null 2>&1
