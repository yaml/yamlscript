include ../common/base.mk
include $(COMMON)/binding.mk

PYTHON-VER := 3.13
PYTHON-VENV := $(ROOT)/python/.venv
PYTHON-VENV-SETUP := \
  pip install pytest pyyaml setuptools twine

include $(MAKES)/python.mk
include $(MAKES)/shell.mk

VENV := source $(PYTHON-VENV)/bin/activate

export PYTHONPATH := \
  $(ROOT)/python/lib:$(PYTHON-VENV)/lib/python$(PYTHON-VER)/site-packages


#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef PYTHON
test:: test-pytest test-ffi
endif

test-pytest: $(PYTHON-VENV)
	$(VENV) && \
	  pytest $${TEST_VERBOSE:+'-v'} test/*.py

test-ffi: $(LIBYS-SO-FQNP)
	$(VENV) && \
	  $(PYTHON) \
	    -c 'import yamlscript; \
	        print(yamlscript.YAMLScript().load("inc: 41"))' && \
	  $(PYTHON) \
	    -c 'import yamlscript; \
	        print(yamlscript.YAMLScript().load("!ys-0\ninc: 41"))'

pkg-test: venv
	$(MAKE) clean
	$(MAKE) test
	$(MAKE) dist
	pip install dist/yamlscript-*.tar.gz
	tar xzf dist/yamlscript-*.tar.gz
	cat yamlscript-*/PKG-INFO

dist: venv MANIFEST.in .long_description.md
	( \
	  $(VENV) && \
	  $(PYTHON) setup.py sdist \
	)

release: publish

publish: dist
	( \
	  $(VENV) && \
	  twine upload --verbose --repository yamlscript dist/yamlscript-*.tar.gz \
	)

clean::
	$(RM) MANIFEST* .long_description.md
	$(RM) -r dist/ .pytest_cache/ yamlscript-0.*/
	$(RM) -r lib/yamlscript.egg-info/ .eggs/
	$(RM) -r lib/yamlscript/__pycache__/
	$(RM) -r test/__pycache__/
	@echo

realclean::
	$(RM) -r $(PYTHON-VENV)

venv: $(PYTHON-VENV)

MANIFEST.in:
	echo 'include ReadMe.md' > $@
	echo 'include .long_description.md' >> $@

.long_description.md: ReadMe.md
	cat $< | \
	    grep -A999 '## Synopsis' | \
	    grep -B999 '## Features' | \
	    head -n-2 \
	> $@
