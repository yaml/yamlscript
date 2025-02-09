include ../common/base.mk
include $(COMMON)/python.mk

export ROOT

CONFIG := mkdocs.yml

MKDOCS_MATERIAL_VERSION := 9.5.50
MKDOCS_MATERIAL_REPO := https://github.com/squidfunk/mkdocs-material

PYTHON_VENV := $(ROOT)/www/.venv
VENV := source $(PYTHON_VENV)/bin/activate

DOC_FILES := $(shell find $(ROOT)/doc -name '*.md' | grep -v /doc/src)
DOC_FILES := $(DOC_FILES:$(ROOT)/doc/%=src/doc/%)

BLOG_FILES := $(shell find $(ROOT)/blog -name '*.md')
BLOG_FILES := $(BLOG_FILES:$(ROOT)/blog/%=src/blog/%)

WATCHER := $(VENV) && watchmedo shell-command
WATCH := \
  Meta \
  doc/src \
  sample \
  www/config.ys \
  www/config \
  www/Makefile \
  END

null :=
space := ${null} ${null}

WATCH := $(subst $(space),;,$(WATCH))

DEPS := \
  $(PYTHON_VENV) \
  $(CONFIG) \
  gen-doc \
  $(DOC_FILES) \
  $(BLOG_FILES) \
  sample \


default::

deps: line1 $(DEPS) line2

ifeq (live,$(website))
  YS_WWW_DOMAIN := yamlscript.org
  YS_WWW_REMOTE := origin
  YS_WWW_BRANCH ?= gh-pages
else ifeq (stage,$(website))
  export YS_WWW_DEV := true
  YS_WWW_DOMAIN := stage.yamlscript.org
  YS_WWW_REMOTE := stage
  YS_WWW_BRANCH := site
endif

build:: $(DEPS)
	$(RM) -r site
	git worktree add -f site
	$(RM) -r site/*
	$(VENV) && mkdocs build
	echo $(YS_WWW_DOMAIN) > site/CNAME
	git -C site add -A

serve: deps watch
	$(VENV) && mkdocs serve

deps-update: deps-update-notify deps

deps-update-notify:
	: *** Rebuilding dependencies ***


# XXX - See 'mkdocs gh-deploy' for a more standard way to do this
# Options remote_branch and remote_name are used for gh-deploy
ifeq (,$(YS_WWW_REMOTE))
publish:
	$(error Use 'make publish website=<live|stage>' to publish)
else
publish: build
	-git -C site commit -m "Publish $$(date)"
	git -C site push $(YS_WWW_REMOTE) HEAD:$(YS_WWW_BRANCH) --force
endif

watchmedo-help:
	$(WATCHER) --help

watch:
	cd .. && \
	$(WATCHER) \
	  --command='$(MAKE) -C www deps-update' \
	  --patterns='$(WATCH)' \
	  --timeout=2 \
	  --recursive \
	  --wait \
	  --drop \
	  &

material:
	git \
	  -c advice.detachedHead=false \
	  clone \
	  --quiet \
	  --depth 1 \
	  --branch $(MKDOCS_MATERIAL_VERSION) \
	  $(MKDOCS_MATERIAL_REPO) $@
	printf '%s\n' material/* | \
	  grep -Ev '/(docs|material|mkdocs.yml)' | \
	  xargs $(RM) -r
	$(RM) -r $@/.git
	ln -s $@/material/templates mt

override: material
ifeq (,$(f))
	@echo 'f=<file> is not set'
	@exit 1
endif
	cp $</$</templates/$f theme/$f

pip-install: $(PYTHON_VENV)
ifeq (,$(m))
	@echo 'm=<module> is not set'
	@exit 1
endif
	$(VENV) && pip install $m
	$(VENV) && pip freeze > requirements.txt

clean::
	$(RM) $(CONFIG) sample
	$(RM) src/doc/*.md src/blog/*.md src/blog/**/*.md
	$(RM) -r site

realclean:: clean
	$(RM) -r $(PYTHON_VENV) material
	rm -f mt

$(VENV_DIR): $(PYTHON_VENV)

$(PYTHON_VENV):
	$(PYTHON) -m venv $@
	$(VENV) && pip install -r requirements.txt

# YS doesn't support !!python tags yet.
# This hack is a workaround to preserve them.
YS_YAML_TAG_HACK := perl -pe 's{: \+!}{: !}'

mkdocs.yml: config.ys ../Meta config/*
	@( \
	  set -euo pipefail; \
	  echo "# DO NOT EDIT - GENERATED FROM '$<'"; \
	  echo; \
	  ys -Y $< | $(YS_YAML_TAG_HACK) \
	) > $@.tmp
	@if ! [[ -s $@.tmp ]]; then \
	  echo "*** Error: failed to generate $@"; \
	  $(RM) -f $@.tmp; \
	  exit 1; \
	elif diff $@.tmp $@ &>/dev/null; then \
		echo "*** No changes to $@"; \
	  $(RM) $@.tmp; \
	else \
	  echo "*** Updated $@"; \
	  mv $@.tmp $@; \
	fi

gen-doc:
	$(MAKE) --no-print-directory -C $(ROOT)/doc build

src/doc/%.md:
	@mkdir -p src/doc
	ln -fs $(@:src/doc/%.md=$(ROOT)/doc/%.md) $@

src/blog/%.md:
	@mkdir -p src/blog src/blog/advent-2023
	ln -fs $(@:src/blog/%.md=$(ROOT)/blog/%.md) $@

sample:
	[[ -h $@ ]] || ln -s ../$@ $@

line1 line2:
	@echo =======================================================================

# yamlscript-1920x1080.png: src/images/yamlscript.svg
# 	svgexport $< $@ 1920:1080 pad
