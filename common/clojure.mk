#------------------------------------------------------------------------------
# Set Clojure specific variables:
#------------------------------------------------------------------------------

YAMLSCRIPT_CORE_INSTALLED := \
  $(MAVEN_REPOSITORY)/yamlscript/core/maven-metadata-local.xml

YAMLSCRIPT_CORE_SRC := \
  $(ROOT)/core/src/yamlscript/*.clj \
  $(ROOT)/core/src/ys/*.clj \

ifdef w
  export WARN_ON_REFLECTION := 1
endif

LEIN := $(BUILD_BIN)/lein

LEIN_URL := \
  https://codeberg.org/leiningen/leiningen/raw/branch/stable/bin/lein

LEIN_COMMANDS := \
  check \
  classpath \
  compile \
  deps \
  run \
  show-profiles \

define HUMANE_TEST_INIT
(do \
  (require 'pjstadig.humane-test-output) \
  (pjstadig.humane-test-output/activate!) \
  (require 'yamlscript.test-runner))
endef

LEIN_REPL_OPTIONS := \
  update-in :dependencies conj '[nrepl,"1.0.0"]' -- \
  update-in :plugins conj '[cider/cider-nrepl,"0.28.5"]' -- \
  update-in '[:repl-options,:nrepl-middleware]' \
    conj '["cider.nrepl/cider-middleware"]' -- \

# For lein to be quieter when YS_QUIET is set:
ifdef YS_QUIET
  export HTTP_CLIENT := curl -sfL -o
endif


#------------------------------------------------------------------------------
# Common Clojure Targets
#------------------------------------------------------------------------------

clean::
	$(RM) -r .lein-*
	$(RM) -r reports/ target/

realclean:: clean

distclean:: nrepl-stop
	$(RM) -r .calva/ .clj-kondo/ .cpcache/ .lsp/ .vscode/ .portal/

$(LEIN): $(JAVA_INSTALLED) | $(BUILD_BIN)
	$(call need-curl)
	$(CURL) -o $@ $(LEIN_URL)
	chmod +x $@


# Leiningen targets
$(LEIN_COMMANDS):: $(LEIN)
	$< $@

deps-graph:: $(LEIN)
	$< deps :tree


# Build/GraalVM targets
force:
	$(RM) $(YAMLSCRIPT_CORE_INSTALLED)

$(YAMLSCRIPT_CORE_INSTALLED): $(YAMLSCRIPT_CORE_SRC)
	$(MAKE) -C $(ROOT)/core install


# Maven targets

$(MAVEN_DOWNLOAD):
	$(call need-curl)
	$(CURL) -o $@ $(MAVEN_URL)

$(MAVEN_INSTALLED): $(MAVEN_DOWNLOAD)
	(cd $(YS_TMP) && tar xzf $<)
	touch $@


# REPL/nREPL management targets
repl:: $(LEIN) repl-deps
ifneq (,$(wildcard .nrepl-pid))
	@echo "Connecting to nREPL server on port $$(< .nrepl-port)"
	$< repl :connect
endif

repl-deps::

nrepl+: nrepl-stop nrepl

nrepl: .nrepl-pid
	@echo "nREPL server running on port $$(< .nrepl-port)"

nrepl-stop:
ifneq (,$(wildcard .nrepl-pid))
	-@( \
	  pid=$$(< .nrepl-pid); \
	  read -r pid1 <<<"$$(ps --ppid $$pid -o pid=)"; \
	  read -r pid2 <<<"$$(ps --ppid $$pid1 -o pid=)"; \
	  set -x; \
	  kill -9 $$pid $$pid1 $$pid2; \
	)
	$(RM) .nrepl-*
endif

ifdef PORT
  repl-port := :port $(PORT)
endif

.nrepl-pid: $(LEIN) repl-deps
	( \
	  $< $(LEIN_REPL_OPTIONS) repl :headless $(repl-port) & \
	  echo $$! > $@ \
	)
	@( \
	  while [[ ! -f .nrepl-port ]]; do \
	    sleep 0.3; \
	  done; \
	)
