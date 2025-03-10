include ../common/base.mk
include $(COMMON)/python.mk

export ROOT

CONFIG := mkdocs.yml

PYTHON_VENV := $(ROOT)/www/.venv
VENV := source $(PYTHON_VENV)/bin/activate

MD_FILES := $(shell find ../doc -name '*.md' | sort)
MDYS_FILES := $(shell find ../doc src -name '*.mdys' | sort)
MDYSMD_FILES := $(MDYS_FILES:%.mdys=%.md)
MD_FILES += $(MDYS_FILES)
MD_FILES := $(MD_FILES:../doc/%=src/doc/%)
MD_FILES := $(MD_FILES:%.mdys=%.md)

BLOG_FILES := $(shell find ../blog -name '*.md' | sort)
BLOG_FILES := $(BLOG_FILES:../blog/%=src/blog/posts/%)

WATCHER := $(VENV) && watchmedo shell-command
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
  $(PYTHON_VENV) \
  $(CONFIG) \
  $(MD_FILES) \
  $(BLOG_FILES) \
  sample \


default::

deps: line1 $(DEPS) line2

ifeq (live,$(website))
  export YS_WWW_LIVE := true
  YS_WWW_DOMAIN := yamlscript.org
  YS_WWW_REMOTE := origin
  YS_WWW_BRANCH ?= gh-pages
else ifeq (stage,$(website))
  YS_WWW_DOMAIN := stage.yamlscript.org
  YS_WWW_REMOTE := stage
  YS_WWW_BRANCH := site
endif

.PHONY: site
build site:: $(DIRS) $(DEPS)
	$(RM) -r site
	git worktree add -f site
	$(RM) -r site/*
	$(VENV) && mkdocs build
	echo $(YS_WWW_DOMAIN) > site/CNAME
	git -C site add -A

serve: $(DIRS) $(DEPS) watch
	$(VENV) && mkdocs serve

deps-update: deps-update-notify deps

deps-update-notify:
	: *** Rebuilding dependencies ***

dirs: $(DIRS)


# XXX - See 'mkdocs gh-deploy' for a more standard way to do this
# Options remote_branch and remote_name are used for gh-deploy
ifeq (,$(YS_WWW_REMOTE))
publish:
	$(error Use 'make publish website=<live|stage>' to publish)
else
publish: build
	-git -C site commit -m "Publish $$(date)"
	git -C site push $(YS_WWW_REMOTE) HEAD:$(YS_WWW_BRANCH) --force
	@echo
	@echo "Published to https://$(YS_WWW_DOMAIN)"
	@echo
endif

watchmedo-help:
	$(WATCHER) --help | less

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
	  --wait \
	  --drop \
	  &

material: $(PYTHON_VENV)
	ln -s $(PYTHON_VENV)/lib/python*/site-packages/material $@

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

pip-install: $(PYTHON_VENV)
ifeq (,$(m))
	@echo 'm=<module> is not set'
	@exit 1
endif
	$(VENV) && pip install $m
	$(VENV) && pip freeze > requirements.txt

clean::
	killall watchmedo || true
	$(RM) src/index.md src/faq.md
	$(RM) $(CONFIG) sample $T material
	$(RM) -r site src/doc src/blog/posts .cache

realclean:: clean
	$(RM) -r $(PYTHON_VENV)

$(VENV_DIR): $(PYTHON_VENV)

$(PYTHON_VENV):
	$(PYTHON) -m venv $@
	$(VENV) && pip install -r requirements.txt

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

# yamlscript-1920x1080.png: src/image/yamlscript.svg
# 	svgexport $< $@ 1920:1080 pad
