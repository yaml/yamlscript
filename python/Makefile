include ../common/base.mk
include $(COMMON)/binding.mk

PYTHON-VER := 3.13
PYTHON-VENV := $(ROOT)/python/.venv
PYTHON-VENV-SETUP := \
  uv pip install --python $(PYTHON-VENV) pytest pyyaml setuptools twine

include $(MAKES)/python.mk
include $(MAKES)/shell.mk

VENV := source $(PYTHON-VENV)/bin/activate

ifdef PYPI_API_TOKEN
TWINE-UPLOAD := \
  TWINE_USERNAME=__token__ \
  TWINE_PASSWORD=$(PYPI_API_TOKEN) \
  twine upload --verbose dist/yamlscript-*.tar.gz
else
TWINE-UPLOAD := \
  twine upload --verbose --repository yamlscript dist/yamlscript-*.tar.gz
endif

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
	  python \
	    -c 'import yamlscript; \
	        print(yamlscript.YAMLScript().load("inc: 41"))' && \
	  python \
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
	  python setup.py sdist \
	)

release: publish

publish: dist
	( \
	  $(VENV) && \
	  $(TWINE-UPLOAD) \
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
	awk '\
	  /^## Synopsis/ { found = 1 } \
	  found && /^## Features/ { exit } \
	  found { print } \
	' $< > $@
