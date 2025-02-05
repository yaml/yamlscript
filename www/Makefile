include ../common/base.mk
include $(COMMON)/python.mk

export ROOT

CONFIG := mkdocs.yml

MATERIAL_VERSION := 9.5.50
MATERIAL_REPO := https://github.com/squidfunk/mkdocs-material

PYTHON_VENV := $(ROOT)/www/.venv
VENV := source $(PYTHON_VENV)/bin/activate

DOC_FILES := $(shell find $(ROOT)/doc -name '*.md' | grep -v /doc/src)
DOC_FILES := $(DOC_FILES:$(ROOT)/doc/%=src/doc/%)

BLOG_FILES := $(shell find $(ROOT)/blog -name '*.md')
BLOG_FILES := $(BLOG_FILES:$(ROOT)/blog/%=src/blog/%)

WATCHER := $(shell command -v simple-file-watch)

DEPS := \
  $(PYTHON_VENV) \
  $(CONFIG) \
  gen-doc \
  $(DOC_FILES) \
  $(BLOG_FILES) \
  sample \

YS_WWW_DOMAIN_NAME ?= yamlscript.org
YS_WWW_ORIGIN ?= origin
ifneq (,$(stage))
  YS_WWW_DOMAIN_NAME := stage.yamlscript.org
  YS_WWW_ORIGIN := stage
endif


default::

deps: line1 $(DEPS) line2

line1 line2:
	@echo =======================================================================

build:: $(DEPS)
	$(RM) -r site
	git worktree add -f site
	$(RM) -r site/*
	$(VENV) && mkdocs build
	echo $(YS_WWW_DOMAIN_NAME) > site/CNAME
	git -C site add -A

serve: deps watch
	$(VENV) && mkdocs serve

# XXX - See 'mkdocs gh-deploy' for a more standard way to do this
# Options remote_branch and remote_name are used for gh-deploy
publish: build
ifeq (,$(stage))
	$(error NOT READY FOR PRIMETIME YET)
endif
	-git -C site commit -m "Publish $$(date)"
	git -C site push $(YS_WWW_ORIGIN) HEAD:site

watch:
ifneq (,$(WATCHER))
	$(WATCHER) \
	  --delay=1000 \
	  --recursive \
	  --path="$$ROOT/Meta" \
	  --path="$$ROOT/doc/" \
	  --path="$$ROOT/sample/" \
	  --path="$$ROOT/www/config.ys" \
	  --path="$$ROOT/www/config/" \
	  --path="$$ROOT/www/Makefile" \
	  --command='make deps' &
endif

material:
	git \
	  -c advice.detachedHead=false \
	  clone \
	  --quiet \
	  --depth 1 \
	  --branch $(MATERIAL_VERSION) \
	  $(MATERIAL_REPO) $@
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
PYTHON_TAG_HACK := perl -pe 's{: (python/name:material\.)}{: !!$$1}'

mkdocs.yml: config.ys ../Meta config/*
	: *** Rebuild $@ from $< ***
	@( \
	  set -euo pipefail; \
	  echo "# DO NOT EDIT - GENERATED FROM '$<'"; \
	  echo; \
	  ys -Y $< | $(PYTHON_TAG_HACK) \
	) > $@.tmp
	@if [[ -s $@.tmp ]] && diff $@.tmp $@ &>/dev/null; then \
	  $(RM) $@.tmp; \
	else \
	  echo "Updated $@"; \
	  mv $@.tmp $@; \
	fi

gen-doc:
	$(MAKE) --no-print-directory -C $(ROOT)/doc build

src/doc/%.md: $(ROOT)/doc/%.md src/doc
	ln -fs $< $@

src/blog/%.md: $(ROOT)/blog/%.md src/blog src/blog/advent-2023
	ln -fs $< $@

src/doc src/blog src/blog/advent-2023:
	mkdir -p $@

sample:
	ln -s ../$@ $@

# yamlscript-1920x1080.png: src/images/yamlscript.svg
# 	svgexport $< $@ 1920:1080 pad
