#------------------------------------------------------------------------------
# Set Clojure / Java specific variables:
#------------------------------------------------------------------------------

export JAVA_HOME := $(GRAALVM_HOME)
export PATH := $(GRAALVM_HOME)/bin:$(PATH)

YAMLSCRIPT_LANG_INSTALLED := \
  $(HOME)/.m2/repository/yamlscript/core/maven-metadata-local.xml
YAMLSCRIPT_CORE_SRC := \
  ../core/src/yamlscript/*.clj \
  ../core/src/ys/*.clj \

ifdef w
  export WARN_ON_REFLECTION := 1
endif

LEIN := $(BUILD_BIN)/lein
LEIN_URL := https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein

LEIN_COMMANDS := \
  check \
  classpath \
  compile \
  deps \
  deploy \
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

#------------------------------------------------------------------------------
# Common Clojure Targets
#------------------------------------------------------------------------------

clean::
	$(RM) pom.xml Dockerfile
	$(RM) -r .lein-*
	$(RM) -r reports/ target/

distclean:: nrepl-stop
	$(RM) -r .calva/ .clj-kondo/ .cpcache/ .lsp/ .vscode/ .portal/

$(LEIN): $(BUILD_BIN) $(GRAALVM_INSTALLED)
ifeq (,$(CURL))
	$(error *** 'curl' is required but not installed)
endif
	$(CURL) -L -o $@ $(LEIN_URL)
	chmod +x $@

$(BUILD_BIN):
	mkdir -p $@

# Leiningen targets
$(LEIN_COMMANDS):: $(LEIN)
	$< $@

deps-graph:: $(LEIN)
	$< deps :tree

# Build/GraalVM targets
force:
	$(RM) $(YAMLSCRIPT_LANG_INSTALLED)

$(YAMLSCRIPT_LANG_INSTALLED): $(YAMLSCRIPT_CORE_SRC)
	$(MAKE) -C ../core install

$(GRAALVM_INSTALLED): $(GRAALVM_DOWNLOAD)
	tar xzf $<
	mv graalvm-* $(GRAALVM_PATH)
	touch $@

$(GRAALVM_DOWNLOAD):
ifeq (,$(CURL))
	$(error *** 'curl' is required but not installed)
endif
	$(CURL) -L -o $@ $(GRAALVM_URL)

# REPL/nREPL management targets
repl:: $(LEIN) repl-deps
ifneq (,$(wildcard .nrepl-pid))
	@echo "Connecting to nREPL server on port $$(< .nrepl-port)"
	$< repl :connect
endif

repl-deps::

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

.nrepl-pid: $(LEIN)
	( \
	  $< $(LEIN_REPL_OPTIONS) repl :headless $(repl-port) & \
	  echo $$! > $@ \
	)
	@( \
	  while [[ ! -f .nrepl-port ]]; do \
	    sleep 0.3; \
	  done; \
	)
