include ../common/base.mk
include $(COMMON)/python.mk

export ROOT

PYTHON_VENV := $(ROOT)/www/.venv
VENV := source $(PYTHON_VENV)/bin/activate
CONFIG := mkdocs.yml

DOC_FILES := $(shell find $(ROOT)/doc -name '*.md' | grep -v /doc/src)
DOC_FILES := $(DOC_FILES:$(ROOT)/doc/%=src/doc/%)

BLOG_FILES := $(shell find $(ROOT)/blog -name '*.md')
BLOG_FILES := $(BLOG_FILES:$(ROOT)/blog/%=src/blog/%)

WATCHER := $(shell command -v simple-file-watch)
WATCHER_PID := watcher.pid

DEPS := \
  $(PYTHON_VENV) \
  $(CONFIG) \
  gen-doc \
  $(DOC_FILES) \
  $(BLOG_FILES) \

YS_WWW_DOMAIN_NAME ?= yamlscript.org
YS_WWW_ORIGIN ?= origin
ifneq (,$(stage))
YS_WWW_DOMAIN_NAME := yamlys.org
YS_WWW_ORIGIN := stage
endif


default::

files: $(DEPS)
	@echo =======================================================================

build:: $(DEPS)
	$(RM) -r site
	git worktree add -f site
	$(RM) -r site/*
	$(VENV) && mkdocs build
	echo $(YS_WWW_DOMAIN_NAME) > site/CNAME
	git -C site add -A

serve: files watch
	$(VENV) && mkdocs serve

# XXX - See 'mkdocs gh-deploy' for a more standard way to do this
# Options remote_branch and remote_name are used for gh-deploy
publish: build
	-git -C site commit -m "Publish $$(date)"
	git -C site push $(YS_WWW_ORIGIN) HEAD:site

watch:
ifneq (,$(wildcard $(WATCHER_PID)))
	-kill -9 $$(< $(WATCHER_PID))
	rm -f $(WATCHER_PID)
endif
	$(WATCHER) \
	  --delay=1000 \
	  --recursive \
	  --path="$$ROOT/Meta" \
	  --path="$$ROOT/doc/src/" \
	  --path="$$ROOT/www/mkdocs.ys" \
		--path="$$ROOT/www/Makefile" \
	  --command='make files' & \
	echo $$! > watcher.pid

stop-watch:
ifneq (,$(wildcard $(WATCHER_PID)))
	-kill -9 $$(< $(WATCHER_PID))
	rm -f $(WATCHER_PID)
endif

clean:: stop-watch
	$(RM) $(CONFIG)
	$(RM) src/doc/*.md src/blog/*.md src/blog/**/*.md
	$(RM) -r site

realclean:: clean
	$(RM) -r $(PYTHON_VENV)

$(VENV_DIR): $(PYTHON_VENV)

$(PYTHON_VENV):
	$(PYTHON) -m venv $@
	$(VENV) && pip install -r requirements.txt

mkdocs.yml: mkdocs.ys ../Meta
	echo "# DO NOT EDIT - GENERATED FROM '$<'" > $@
	echo >> $@
	ys -Y $< >> $@

gen-doc:
	$(MAKE) -C $(ROOT)/doc build

src/doc/%.md: $(ROOT)/doc/%.md
	ln -fs $< $@

src/blog/%.md: $(ROOT)/blog/%.md
	ln -fs $< $@

# yamlscript-1920x1080.png: src/images/yamlscript.svg
# 	svgexport $< $@ 1920:1080 pad
