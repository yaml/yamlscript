include ../common/base.mk
include $(COMMON)/binding.mk

PYTHON-VER := 3.13
PYTHON-VENV := $(ROOT)/python/.venv
PYTHON-VENV-SETUP := \
  uv pip install --python $(PYTHON-VENV) pytest pyyaml setuptools twine wheel

include $(MAKES)/python.mk
include $(MAKES)/shell.mk

ifeq ($(OS-NAME),windows)
VENV := source $(PYTHON-VENV)/Scripts/activate
else
VENV := source $(PYTHON-VENV)/bin/activate
endif

ifdef PYPI_API_TOKEN
TWINE-UPLOAD := \
  TWINE_USERNAME=__token__ \
  TWINE_PASSWORD=$(PYPI_API_TOKEN) \
  twine upload --verbose dist/yamlscript-*
TWINE-WHEEL-UPLOAD := \
  TWINE_USERNAME=__token__ \
  TWINE_PASSWORD=$(PYPI_API_TOKEN) \
  twine upload --verbose --skip-existing dist/*.whl
else
TWINE-UPLOAD := \
  twine upload --verbose --repository yamlscript dist/yamlscript-*
TWINE-WHEEL-UPLOAD := \
  twine upload --verbose --skip-existing --repository yamlscript dist/*.whl
endif

WHEEL-PLAT-linux-int64 := manylinux_2_31_x86_64
WHEEL-PLAT-macos-arm64 := macosx_14_0_arm64
WHEEL-PLAT-windows-int64 := win_amd64
WHEEL-PLAT := $(WHEEL-PLAT-$(OS-ARCH))
WHEEL-LIBYS-DIR := lib/yamlscript/libys
WHEEL-LIBYS := $(WHEEL-LIBYS-DIR)/$(notdir $(LIBYS-SO-FQNP))
RELEASE-WHEEL-VERSION := $(or $(n),$(YS_VERSION))
RELEASE-WHEEL-DIR ?= dist/release-assets
RELEASE-WHEEL-ARCHIVES := \
  $(wildcard $(RELEASE-WHEEL-DIR)/libys-$(RELEASE-WHEEL-VERSION)-*.tar.xz) \
  $(wildcard $(RELEASE-WHEEL-DIR)/libys-$(RELEASE-WHEEL-VERSION)-*.zip)

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

wheel: venv MANIFEST.in .long_description.md $(WHEEL-LIBYS)
	@test -n "$(WHEEL-PLAT)" || { \
	  echo "No Python wheel platform tag for $(OS-ARCH)"; \
	  exit 1; \
	}
	( \
	  $(VENV) && \
	  python setup.py bdist_wheel --plat-name $(WHEEL-PLAT) \
	)

$(WHEEL-LIBYS): $(LIBYS-SO-FQNP)
	mkdir -p $(dir $@)
	cp -p $< $@

wheels-from-release: venv MANIFEST.in .long_description.md
	@test -n "$(RELEASE-WHEEL-ARCHIVES)" || { \
	  echo 'No libys release archives found in $(RELEASE-WHEEL-DIR)'; \
	  exit 1; \
	}
	@set -e; \
	  for archive in $(RELEASE-WHEEL-ARCHIVES); do \
	    name=$$(basename "$$archive"); \
	    name=$${name%.tar.xz}; \
	    name=$${name%.zip}; \
	    relplat=$${name#libys-$(RELEASE-WHEEL-VERSION)-}; \
	    case "$$relplat" in \
	      linux-x64) plat=manylinux_2_31_x86_64; libpat='libys.so.*' ;; \
	      macos-aarch64) plat=macosx_14_0_arm64; libpat='libys.dylib.*' ;; \
	      windows-x64) plat=win_amd64; libpat='libys.dll' ;; \
	      *) echo "Skipping unsupported Python wheel asset: $$name"; \
	         continue ;; \
	    esac; \
	    rm -rf build $(WHEEL-LIBYS-DIR); \
	    mkdir -p $(WHEEL-LIBYS-DIR); \
	    tmp=$$(mktemp -d); \
	    case "$$archive" in \
	      *.zip) unzip -q "$$archive" -d "$$tmp" ;; \
	      *) tar -C "$$tmp" -xf "$$archive" ;; \
	    esac; \
	    lib=$$(find "$$tmp" -type f -name "$$libpat" | sort | head -n1); \
	    test -n "$$lib" || { \
	      echo "No $$libpat found in $$archive"; \
	      exit 1; \
	    }; \
	    cp -p "$$lib" $(WHEEL-LIBYS-DIR)/; \
	    echo "Building Python wheel for $$relplat"; \
	    $(VENV) && python setup.py bdist_wheel --plat-name "$$plat"; \
	    rm -rf "$$tmp"; \
	  done; \
	  rm -rf build $(WHEEL-LIBYS-DIR)

release: publish

publish: dist
	( \
	  $(VENV) && \
	  $(TWINE-UPLOAD) \
	)

publish-wheels: venv
	@test "$$(find dist -maxdepth 1 -name '*.whl' | wc -l)" -gt 0 || { \
	  echo 'No Python wheels found in python/dist'; \
	  exit 1; \
	}
	( \
	  $(VENV) && \
	  $(TWINE-WHEEL-UPLOAD) \
	)

clean::
	$(RM) MANIFEST* .long_description.md
	$(RM) -r dist/ .pytest_cache/ yamlscript-0.*/
	$(RM) -r build/ $(WHEEL-LIBYS-DIR)/
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
