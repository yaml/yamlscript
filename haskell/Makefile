include ../common/base.mk
include $(COMMON)/binding.mk
include $(MAKES)/cabal.mk      # includes ghc.mk
include $(MAKES)/shell.mk

CABAL-INDEX := $(CABAL_DIR)/packages/hackage.haskell.org/01-index.tar.gz

HACKAGE-TOKEN ?= $(HACKAGE_TOKEN)
ifeq (,$(HACKAGE-TOKEN))
ifneq (,$(shell command -v ys))
ifneq (,$(wildcard ~/.yamlscript-secrets.yaml))
HACKAGE-TOKEN := \
  $(shell ys -e '.hackage.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif
endif
endif

HASKELL-DIST := \
  dist-newstyle/sdist/yamlscript-$(YAMLSCRIPT_VERSION).0.tar.gz

HASKELL-DEPS := \
  $(GHC) \
  $(CABAL) \
  $(CABAL-INDEX) \
  yamlscript.cabal \
	License \

LIBYS := /tmp/libys

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: test-cabal test-ffi

test-cabal: $(HASKELL-DEPS) $(LIBYS)
	cabal test --test-show-details=direct

test-ffi: $(HASKELL-DEPS) $(LIBYS)
	cabal run yamlscript-test -- \
	  -c 'import YAMLScript; \
	      print =<< loadYAMLScript "inc: 41"' && \
	cabal run yamlscript-test -- \
	  -c 'import YAMLScript; \
	      print =<< loadYAMLScript "!ys-0\ninc: 41"'

pkg-test: $(HASKELL-DEPS)
	$(MAKE) clean
	$(MAKE) test
	cabal sdist
	cabal exec -- cabal install $(HASKELL-DIST)

ls: dist
	ls -l $(HASKELL-DIST)

release: dist
ifndef HACKAGE-TOKEN
	@echo "HACKAGE-TOKEN is not set"
	@exit 1
endif
	cabal upload \
	  --publish \
	  --verbose \
	  --token='$(HACKAGE-TOKEN)' \
	  $(HASKELL-DIST)

check: $(HASKELL-DEPS)
	cabal $@

dist: $(HASKELL-DEPS)
	cabal check
	cabal sdist

clean::
	$(RM) yamlscript.cabal License
	$(RM) -r dist-newstyle/ $(LIBYS)

$(CABAL-INDEX): $(CABAL)
	cabal update

ifdef YS_RELEASE_USE_INSTALLED_YS
yamlscript.cabal: cabal.ys
else
yamlscript.cabal: cabal.ys $(YS)
endif
	$(YS) $< > $@

License: ../License
	cp $< $@

$(LIBYS): $(LIBYS-SO-FQNP)
	mkdir -p $(LIBYS)
	cp -r ../libys/lib $(LIBYS)/
