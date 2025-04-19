include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/native.mk
include $(COMMON)/vars-cli.mk

BPAN_LOCAL := .bpan
BPAN_REPO_URL := https://github.com/bpan-org/bpan

test := test/


#------------------------------------------------------------------------------
build:: $(CLI_DEPS)

install: $(CLI_DEPS)
	mkdir -p $(PREFIX)/bin
	install -m 755 $(CLI_BIN) $(PREFIX)/bin/
	ln -fs $(notdir $(CLI_BIN)) \
	  $(PREFIX)/$(CLI_BIN:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $(CLI_BIN)) \
	  $(PREFIX)/$(CLI_BIN:%-$(YS_VERSION)=%)
	install -m 755 $(CLI_BIN_BASH) \
	  $(PREFIX)/bin/

jar: $(CLI_JAR)

test: test-run

test-all: test-unit test-run

test-unit: $(CORE_INSTALLED)
	$(LEIN) test $(lein-test)

test-run: $(CLI_DEPS) $(BPAN_LOCAL)
	prove $${TEST_VERBOSE:+'-v'} $(test)

repl-deps:: $(CORE_INSTALLED)

clean::
	$(RM) -r .cpcache/

realclean::
	$(RM) -r bin lib $(BPAN_LOCAL)

$(BPAN_LOCAL):
	git clone --depth=1 $(BPAN_REPO_URL) $@

$(CLI_BIN): $(CLI_JAR)
ifneq (true,$(LIBZ))
	$(error *** \
	  The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	$(TIME) \
	native-image \
	  -H:ConfigurationFileDirectories=config/ \
	  $(NATIVE_OPTS) \
	\
	  -jar $< \
	  -o $@
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%)
	touch $@

$(CLI_BIN_BASH): $(CLI_BIN_BASH_SRC)
	mkdir -p $(dir $@)
	cp $< $@
	chmod 755 $@

$(CLI_JAR): $(CLI_JAR_DEPS)
	$(LEIN) uberjar
	touch $@

$(CORE_INSTALLED): $(CORE_JAR)
	$(MAKE) -C $(CORE_DIR) $@

$(CORE_JAR): $(CORE_DEPS)
	$(MAKE) -C $(CORE_DIR) $@
