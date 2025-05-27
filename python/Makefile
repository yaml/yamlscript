include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/python.mk

PYTHON_VENV := $(ROOT)/python/.venv

VENV := source $(PYTHON_VENV)/bin/activate

export PYTHONPATH := $(ROOT)/python/lib


#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef PYTHON
test:: test-pytest test-ffi
endif

test-pytest: $(PYTHON_VENV)
	$(VENV) && \
	  pytest $${TEST_VERBOSE:+'-v'} test/*.py

test-ffi: $(LIBYS_SO_FQNP)
	$(VENV) && \
	  $(PYTHON) \
	    -c 'import yamlscript; \
	        print(yamlscript.YAMLScript().load("inc: 41"))' && \
	  $(PYTHON) \
	    -c 'import yamlscript; \
	        print(yamlscript.YAMLScript().load("!YS-v0\ninc: 41"))'

pkg-test: venv
	$(MAKE) clean
	$(MAKE) test
	$(MAKE) dist
	pip install dist/yamlscript-*.tar.gz
	tar xzf dist/yamlscript-*.tar.gz
	cat yamlscript-*/PKG-INFO



dist: venv MANIFEST.in .long_description.md
	$(VENV) && \
	$(PYTHON) setup.py sdist && \
	cp $(LIBYS_SO_FQNP) ./lib/yamlscript/ && \
	$(PYTHON) setup.py bdist_wheel

release: publish

publish: dist
	( \
	  $(VENV) && \
	  twine upload --verbose --repository yamlscript dist/yamlscript-*.tar.gz dist/yamlscript-*.whl \
	)

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
	$(VENV) && \
	  pip install \
	    pytest \
	    pyyaml \
	    twine \
	    setuptools

MANIFEST.in:
	echo 'include ReadMe.md' > $@
	echo 'include .long_description.md' >> $@

.long_description.md: ReadMe.md
	cat $< | \
	    grep -A999 '## Synopsis' | \
	    grep -B999 '## Features' | \
	    head -n-2 \
	> $@
