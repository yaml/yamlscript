SHELL := bash
ROOT := $(shell pwd)

PYTHON := $(shell command -v python3)
PYTHON ?= $(shell command -v python)

VERSION := $(shell head -n1 setup.py | cut -d"'" -f2)

export PYTHONPATH := $(ROOT)/lib


default:
	@echo $(VERSION)

.PHONY: test
test: .venv
	source .venv/bin/activate && \
	pytest -v test/*.py

pkg-test: venv
	make clean
	make test
	make dist
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

clean:
	rm -f MANIFEST* .long_description.md
	rm -fr dist/ .pytest_cache/ yamlscript-0.*/
	rm -fr lib/yamlscript.egg-info/ .eggs/
	find . -name '__pycache__' | xargs rm -fr

realclean: clean
	rm -fr .venv/

venv: .venv
	@[[ $$VIRTUAL_ENV == $$PWD/.venv ]] || { \
	    echo; \
	    echo "Run 'source .venv/bin/activate'"; \
	    echo; \
	    exit 1; \
	}

.venv:
	$(PYTHON) -mvenv $@
	source .venv/bin/activate && \
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
