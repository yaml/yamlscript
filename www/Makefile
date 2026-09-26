include ../common/base.mk
PYTHON-VENV := $(ROOT)/www/.venv

include $(COMMON)/glojure.mk
include $(MAKES)/node.mk
include $(MAKES)/python.mk
SHELL-DEPS := $(PYTHON-VENV) $(NODE)
SHELL-NAME := makes+venv
include $(MAKES)/shell.mk

CONFIG := mkdocs.yml
MKDOCS := \
  env \
  PYTHONPATH=$(CURDIR)/python \
  PYTHONWARNINGS=ignore::DeprecationWarning \
  mkdocs

MD-FILES := $(shell find ../doc -name '*.md' | sort)
MDYS-FILES := $(shell find ../doc src -name '*.mdys' | sort)
MD-FILES += $(MDYS-FILES)
MD-FILES := $(MD-FILES:../doc/%=src/doc/%)
MD-FILES := $(MD-FILES:%.mdys=%.md)

BLOG-FILES := $(shell find ../blog -name '*.md' | sort)
BLOG-FILES := $(BLOG-FILES:../blog/%=src/blog/posts/%)

WATCHER := watchmedo shell-command
WATCH := \
  Meta \
  doc/ \
  blog/ \
  sample/ \
  util/mdys \
  www/mkdocs.ys \
  www/config/ \
  www/playground/dynamic-examples.yaml \
  www/playground/playground.html \
  www/playground/program-examples.yaml \
  www/src/mdys.ys \
  www/src/*.mdys \
  www/Makefile \
  END

null :=
space := ${null} ${null}

RANDOM-PORT := \
  import socket; \
  sock = socket.socket(); \
  sock.bind(("", 0)); \
  print(sock.getsockname()[1]); \
  sock.close()

T := /tmp/ys-website.tmp

PLAYGROUND-SRC := playground
PLAYGROUND-ASSETS := src/assets/playground
PLAYGROUND-NODE-STAMP := node_modules/.installed
PLAYGROUND-BUNDLE-STAMP := $(PLAYGROUND-ASSETS)/.bundle
PLAYGROUND-RUNTIME-STAMP := $(PLAYGROUND-ASSETS)/.runtime
PLAYGROUND-WASM := $(PLAYGROUND-ASSETS)/runtime.wasm
PLAYGROUND-SUPPORT := $(PLAYGROUND-ASSETS)/wasm_exec.js
PLAYGROUND-RUNTIME-SRC := \
  $(PLAYGROUND-SRC)/playground/main.clj

WATCH := $(subst $(space),;,$(WATCH))

DIRS := \
  src/doc \
  src/blog/posts \

DEPS := \
  $(YS) \
  $(DIRS) \
  $(PYTHON-VENV) \
  $(PLAYGROUND-BUNDLE-STAMP) \
  $(PLAYGROUND-RUNTIME-STAMP) \
  $(CONFIG) \
  $(MD-FILES) \
  $(BLOG-FILES) \
  sample \


default::

deps: line1 $(DEPS) line2

website ?= live
YS-WWW-DOMAIN := yamlscript.org
ifeq (live,$(website))
  export YS_WWW_LIVE := true
  YS-WWW-REMOTE := origin
  YS-WWW-BRANCH ?= gh-pages
else ifeq (stage,$(website))
  YS-WWW-DOMAIN := stage.yamlscript.org
  YS-WWW-REMOTE := stage
  YS-WWW-BRANCH := site
endif

.PHONY: site
build site:: $(DIRS) $(DEPS)
	$(RM) -r site
	git fetch $(YS-WWW-REMOTE) $(YS-WWW-BRANCH)
	git worktree add -f --detach site $(YS-WWW-REMOTE)/$(YS-WWW-BRANCH)
	$(RM) -r site/*
	$(MKDOCS) build
	echo $(YS-WWW-DOMAIN) > site/CNAME
	git -C site add -A

serve: $(DIRS) $(DEPS) watch
	@port='$(PORT)'; \
	  if [[ $$port == random ]]; then \
	    port=$$($(PYTHON) -c '$(RANDOM-PORT)'); \
	  fi; \
	  if [[ -n $$port ]]; then \
	    $(MKDOCS) serve --dev-addr=0.0.0.0:$$port; \
	  else \
	    $(MKDOCS) serve; \
	  fi

deps-update: deps-update-notify deps

deps-update-notify:
	: *** Rebuilding dependencies ***

dirs: $(DIRS)

test: test-playground

test-playground: src/index.md src/play.md
test-playground: $(PLAYGROUND-BUNDLE-STAMP) $(PLAYGROUND-RUNTIME-STAMP)
	npm test
	node $(PLAYGROUND-SRC)/runtime-smoke.cjs \
	  $(PLAYGROUND-WASM) $(PLAYGROUND-SUPPORT)
	compressed=$$(gzip -c $(PLAYGROUND-WASM) | wc -c); \
	  echo "Playground Wasm: $$compressed compressed bytes"; \
	  test $$compressed -le 10485760


# XXX - See 'mkdocs gh-deploy' for a more standard way to do this
# Options remote_branch and remote_name are used for gh-deploy
ifeq (,$(YS-WWW-REMOTE))
publish:
	$(error Use 'make publish website=<live|stage>' to publish)
else
publish: build
	git -C site commit -m "Publish $$(date)"
	git -C site push $(YS-WWW-REMOTE) HEAD:$(YS-WWW-BRANCH)
	@echo
	@echo "Published to https://$(YS-WWW-DOMAIN)"
	@echo
endif

watchmedo-help:
	$(WATCHER) --help | less

# NOTE: Dont use '--wait' here. '--drop' prevents running too much.
#
# https://github.com/gorakhargosh/watchdog/issues/136#issuecomment-40088653
watch:
	: Starting watching: '$(WATCH)'
	@cd .. && \
	$(WATCHER) \
	  --debug-force-polling \
	  --command='\
	    bash -c "\
	      : CHANGED $$watch_src_path $$watch_event_type; \
	      [[ $$watch_event_type =~ ^(created|deleted|modified|moved)$$ ]] && \
	        $(MAKE) -C www deps-update; \
	  "' \
	  --patterns='$(WATCH)' \
	  --recursive \
	  --timeout=2 \
	  --drop \
	  &

material: $(PYTHON-VENV)
	ln -s $</lib/python*/site-packages/material $@

override: material
ifeq (,$(f))
	@echo 'f=<file> is not set'
	@exit 1
endif
ifneq (,$(wildcard theme/partials/$f))
	@echo 'theme/partials/$f already exists'
	@exit 1
endif
	cp $</templates/partials/$f theme/partials/$f

pip-install: $(PYTHON-VENV)
ifeq (,$(m))
	@echo 'm=<module> is not set'
	@exit 1
endif
	pip install $m
	pip freeze > requirements.txt

clean::
	killall watchmedo || true
	$(RM) src/index.md src/play.md src/faq.md
	$(RM) $(CONFIG) sample $T material
	$(RM) $(GO-YAML-GENERATED-STAMP)
	$(RM) -r site src/doc src/blog/posts .cache
	$(RM) -r python/__pycache__

realclean:: clean
	$(RM) -r $(PLAYGROUND-ASSETS) $(PYTHON-VENV) node_modules

$(PLAYGROUND-NODE-STAMP): package.json package-lock.json $(NODE)
	mkdir -p $(@D)
	npm_config_cache=$(LOCAL-CACHE)/npm npm ci
	touch $@

$(PLAYGROUND-BUNDLE-STAMP): \
    Makefile \
    $(PLAYGROUND-NODE-STAMP) \
    $(PLAYGROUND-SRC)/esbuild.mjs \
    $(PLAYGROUND-SRC)/output-format.js \
    $(PLAYGROUND-SRC)/playground.js \
    $(PLAYGROUND-SRC)/playground-worker.js \
    $(PLAYGROUND-SRC)/project.js \
    $(PLAYGROUND-SRC)/url-state.js \
    $(PLAYGROUND-SRC)/virtual-fs.js
	mkdir -p $(@D)
	npm run build
	touch $@

$(PLAYGROUND-RUNTIME-STAMP): \
    Makefile \
    $(GLOAT-RUN-DEPS) \
    $(GLOJURE-PREPARED-STAMP) \
    $(PLAYGROUND-RUNTIME-SRC)
	mkdir -p $(@D)
	$(RM) $@ $(PLAYGROUND-WASM) $(PLAYGROUND-SUPPORT) \
	  $(PLAYGROUND-ASSETS)/runtime.js
	$(RM) -r $(PLAYGROUND-ASSETS)/runtime.html
	</dev/null env -u GOROOT \
	  CGO_ENABLED=0 \
	  GLOAT_YS_RUNTIME=all \
	  GLOAT_MODULE=github.com/yaml/yamlscript \
	  GLOAT_EXTRA_GO_DIR=$(GO-YAML-GENERATED-DIR) \
	  $(GLOAT-RUN) --force --platform=js/wasm --to=js --ext=html \
	  --out=$(PLAYGROUND-ASSETS)/runtime.js \
	  $(PLAYGROUND-RUNTIME-SRC) $(GLOJURE-SRCS)
	mv $(PLAYGROUND-ASSETS)/runtime.js $(PLAYGROUND-WASM)
	$(PERL) -0777 -ne \
	  'm{<script>\s*(.*?)\s*</script>}s or die "runtime script not found\n"; \
	  print $$1' \
	  $(PLAYGROUND-ASSETS)/runtime.html > $(PLAYGROUND-SUPPORT).tmp
	mv $(PLAYGROUND-SUPPORT).tmp $(PLAYGROUND-SUPPORT)
	$(RM) $(PLAYGROUND-ASSETS)/runtime.html
	touch $@

# YS doesn't support !!python tags yet.
# This hack is a workaround to preserve them.
YS_YAML_TAG_HACK := env -u PERL5OPT perl -pe 's{: \+!}{: !}'

$(CONFIG): mkdocs.ys ../Meta config/*
	@( \
	  set -euo pipefail; \
	  echo "# DO NOT EDIT - GENERATED FROM '$<'"; \
	  echo; \
	  ys -Y $< | $(YS_YAML_TAG_HACK) \
	) > $T
	@if ! [[ -s $T ]]; then \
	  echo "*** Error: failed to generate $@"; \
	  $(RM) -f $T; \
	  exit 1; \
	elif diff $T $@ &>/dev/null; then \
	  echo "*** No changes to $@"; \
	  $(RM) $T; \
	else \
	  echo "*** Updated $@"; \
	  mv $T $@; \
	fi

src/%.md: src/%.mdys src/mdys.ys ../util/mdys
	mdys-md $< $@

src/index.md src/play.md: \
    playground/dynamic-examples.yaml \
    playground/playground.html \
    playground/program-examples.yaml
src/index.md src/play.md: $(wildcard ../sample/rosetta-code/*.ys)

src/mdys.ys:

src/doc/%.md: ../doc/%.md
	copy-md $< $@

src/doc/%.md: ../doc/%.mdys ../doc/mdys.ys ../util/mdys
	mdys-md $< $@

src/blog/posts/%.md: ../blog/%.md
	copy-md $< $@

$(DIRS):
	mkdir -p $@

sample:
	[[ -h $@ ]] || ln -s ../$@ $@

line1 line2:
	@echo =======================================================================
