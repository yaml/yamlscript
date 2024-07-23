include ../common/base.mk
include $(COMMON)/binding.mk

LUA := $(shell command -v lua)

export LUA_PATH := $(ROOT)/lua


#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

.PHONY: test
ifdef LUA
test:: test-pytest test-ffi
endif

dist: venv MANIFEST.in .long_description.md
	( \
	  $(ACTIVATE) && \
	  $(PYTHON) setup.py sdist \
	)

release: publish

publish: dist
	@echo FIXME

clean::
	$(RM) MANIFEST* .long_description.md
	$(RM) -r dist/ .pytest_cache/ yamlscript-0.*/
	$(RM) -r lib/yamlscript.egg-info/ .eggs/
	find . -name '__pycache__' | xargs $(RM) -r
	@echo

realclean::
	$(RM) -r $(PYTHON_VENV)

venv: $(PYTHON_VENV)

$(PYTHON_VENV):
	$(PYTHON) -mvenv $@
	$(ACTIVATE) && \
	  pip install \
	    pytest \
	    pyyaml \
	    twine

MANIFEST.in:
	echo 'include ReadMe.md' > $@
	echo 'include .long_description.md' >> $@

.long_description.md: ReadMe.md
	cat $< | \
	    grep -A999 '## Synopsis' | \
	    grep -B999 '## Features' | \
	    head -n-2 \
	> $@

Dockerfile:: $(COMMON) Makefile
	cat \
	  $</docker-from-ubuntu.dockerfile \
	  $</docker-apt-base.dockerfile \
	  $</docker-useradd.dockerfile \
	  $</docker-apt-python.dockerfile \
	  $</docker-deps-python.dockerfile \
	  $</docker-apt-clojure.dockerfile \
	  $</docker-copy-project-deps.dockerfile \
	  $</docker-deps-clojure.dockerfile \
	  $</docker-install-graalvm.dockerfile \
	  $</docker-apt-dev.dockerfile \
	  > $@
