include ../common/base.mk
include ../common/docker.mk

PYTHON := $(shell command -v python3)
PYTHON ?= $(shell command -v python)

ifdef DOCKERENV
  PYTHON_VENV := /tmp/venv
else
  PYTHON_VENV := $(ROOT)/python/.venv
endif

VERSION := $(shell head -n1 setup.py | cut -d"'" -f2)

export PYTHONPATH := $(ROOT)/python/lib

#------------------------------------------------------------------------------
version:
	@echo $(VERSION)

.PHONY: test
test:: test-pytest test-ffi

test-pytest: $(PYTHON_VENV)
	source $</bin/activate && \
	pytest $${TEST_VERBOSE:+'-v'} test/*.py

test-ffi: $(LIBYAMLSCRIPT_SO_FQNP)
	$(PYTHON) -c 'import yamlscript; print(yamlscript.load("inc: 41"))'

$(LIBYAMLSCRIPT_SO_FQNP): $(ROOT)/libyamlscript
	$(MAKE) -C $< build

pkg-test: venv
	$(MAKE) clean
	$(MAKE) test
	$(MAKE) dist
	pip install dist/yamlscript-*.tar.gz
	tar xzf dist/yamlscript-*.tar.gz
	cat yamlscript-*/PKG-INFO

dist: venv MANIFEST.in .long_description.md
	$(PYTHON) setup.py sdist

release: publish tag push

publish: dist
	twine upload -u $${PYPI_USER:-$$USER} dist/*

tag:
	-git add . && git commit -m $(VERSION)
	git tag $(VERSION)

push:
	-git push
	-git push --tag

clean::
	$(RM) MANIFEST* .long_description.md
	$(RM) -r dist/ .pytest_cache/ yamlscript-0.*/
	$(RM) -r lib/yamlscript.egg-info/ .eggs/
	find . -name '__pycache__' | xargs $(RM) -r
	$(RM) -r $(LIBRARY_PATH)
	@echo

realclean::
	$(RM) -r $(PYTHON_VENV)

venv: $(PYTHON_VENV)
	@[[ $$VIRTUAL_ENV == $< ]] || { \
	    echo; \
	    echo "Run 'source $</bin/activate'"; \
	    echo; \
	    exit 1; \
	}

$(PYTHON_VENV):
	$(PYTHON) -mvenv $@
	source $@/bin/activate && \
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
