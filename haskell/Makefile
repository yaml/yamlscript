include ../common/base.mk
include $(COMMON)/binding.mk

GHC-VERSION := 9.12.1
include $(MAKES)/cabal.mk    		# includes ghc.mk
include $(MAKES)/shell.mk

CABAL-INDEX := $(CABAL_DIR)/packages/hackage.haskell.org/01-index.tar.gz

ifneq (,$(shell command -v ys))
ifneq (,$(wildcard ~/.yamlscript-secrets.yaml))
HACKAGE-TOKEN := \
  $(shell ys -e '.hackage.token:say' ~/.yamlscript-secrets.yaml)
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
	      print =<< loadYAMLScript "!YS-v0\ninc: 41"'

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

yamlscript.cabal: cabal.ys $(YS)
	ys $< > $@

License: ../License
	cp $< $@

$(LIBYS): $(LIBYS-SO-FQNP)
	mkdir -p $(LIBYS)
	cp -r ../libys/lib $(LIBYS)/
