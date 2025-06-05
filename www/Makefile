include ../common/base.mk
include $(COMMON)/python.mk

export ROOT

CONFIG := mkdocs.yml

PYTHON-VENV := $(ROOT)/www/.venv
VENV := source $(PYTHON-VENV)/bin/activate
export VIRTUAL_ENV := $(PYTHON-VENV)
export PATH := $(PYTHON-VENV)/bin:$(PATH)

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
  www/src/mdys.ys \
  www/src/*.mdys \
  www/Makefile \
  END

null :=
space := ${null} ${null}

T := /tmp/ys-website.tmp

WATCH := $(subst $(space),;,$(WATCH))

DIRS := \
  src/doc \
  src/blog/posts \

DEPS := \
  build-bin-ys \
  $(DIRS) \
  $(PYTHON-VENV) \
  $(CONFIG) \
  $(MD-FILES) \
  $(BLOG-FILES) \
  sample \


default::

deps: line1 $(DEPS) line2

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
	git worktree add -f site
	$(RM) -r site/*
	$(VENV) && mkdocs build
	echo $(YS-WWW-DOMAIN) > site/CNAME
	git -C site add -A

serve: $(DIRS) $(DEPS) watch
	mkdocs serve

deps-update: deps-update-notify deps

deps-update-notify:
	: *** Rebuilding dependencies ***

dirs: $(DIRS)


# XXX - See 'mkdocs gh-deploy' for a more standard way to do this
# Options remote_branch and remote_name are used for gh-deploy
ifeq (,$(YS-WWW-REMOTE))
publish:
	$(error Use 'make publish website=<live|stage>' to publish)
else
publish: build
	-git -C site commit -m "Publish $$(date)"
	git -C site push $(YS-WWW-REMOTE) HEAD:$(YS-WWW-BRANCH) --force
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
	  --command='\
	    bash -c "\
	      : CHANGED $$watch_src_path $$watch_event_type; \
	      [[ $$watch_event_type == modified ]] && \
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
	$(RM) src/index.md src/faq.md
	$(RM) $(CONFIG) sample $T material
	$(RM) -r site src/doc src/blog/posts .cache

realclean:: clean
	$(RM) -r $(PYTHON-VENV)

$(PYTHON-VENV):
	$(PYTHON) -m venv $@
	pip install -r requirements.txt

# YS doesn't support !!python tags yet.
# This hack is a workaround to preserve them.
YS_YAML_TAG_HACK := perl -pe 's{: \+!}{: !}'

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
