include ../common/base.mk
include $(COMMON)/binding.mk

# Inside the test container (see test.dockerfile) ghc and cabal come
# from the image, along with a pre-compiled dependency store:
ifneq (,$(wildcard /.dockerenv))
HASKELL-DOCKER-INNER := true
else
include $(MAKES)/cabal.mk      # includes ghc.mk
endif

include $(MAKES)/shell.mk

HASKELL-IMAGE ?= ghcr.io/yaml/yamlscript-haskell-test

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

ifdef HASKELL-DOCKER-INNER
HASKELL-DEPS := \
  yamlscript.cabal \
  License \

else
HASKELL-DEPS := \
  $(GHC) \
  $(CABAL) \
  $(CABAL-INDEX) \
  yamlscript.cabal \
	License \

endif

LIBYS := /tmp/libys
# Target the copied .so file, not the directory: the directory can
# pre-exist (empty) in the test container image:
LIBYS-COPY := $(LIBYS)/lib/$(notdir $(LIBYS-SO-FQNP))

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

# Run the tests in the prebaked container when docker is available
# (locally and on CI Linux runners); dependencies never compile:
ifneq (,$(and $(if $(HASKELL-DOCKER-INNER),,x),$(shell command -v docker)))
test:: $(LIBYS-SO-FQNP) yamlscript.cabal License docker-image
	docker run --rm \
	  --user $(USER-UID):$(USER-GID) \
	  --env HOME=/tmp/home \
	  --env CABAL_DIR=/opt/cabal \
	  --volume $(ROOT):$(ROOT) \
	  --workdir $(CURDIR) \
	  $(HASKELL-IMAGE) \
	  make test

# Publish from the container too (the host toolchain path downloads
# ghc and cabal from downloads.haskell.org, which the image already
# has baked in); cabal reads HACKAGE_TOKEN from the env:
release: export HACKAGE_TOKEN := $(HACKAGE-TOKEN)
release: docker-image
	docker run --rm \
	  --user $(USER-UID):$(USER-GID) \
	  --env HOME=/tmp/home \
	  --env CABAL_DIR=/opt/cabal \
	  --env HACKAGE_TOKEN \
	  --volume $(ROOT):$(ROOT) \
	  --workdir $(CURDIR) \
	  $(HASKELL-IMAGE) \
	  make release

docker-image: yamlscript.cabal
	docker image inspect $(HASKELL-IMAGE) > /dev/null 2>&1 || \
	docker pull $(HASKELL-IMAGE) 2> /dev/null || \
	docker build -f test.dockerfile -t $(HASKELL-IMAGE) .

docker-image-build: yamlscript.cabal
	docker build -f test.dockerfile -t $(HASKELL-IMAGE) .

docker-image-push: docker-image-build
	docker push $(HASKELL-IMAGE)
else
test:: test-cabal test-ffi

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
endif

test-cabal: $(HASKELL-DEPS) $(LIBYS-COPY)
	cabal test --test-show-details=direct

test-ffi: $(HASKELL-DEPS) $(LIBYS-COPY)
	cabal run -v0 yamlscript-test -- 'inc: 41' | \
	  grep -q '"inc": 41'
	cabal run -v0 yamlscript-test -- \
	  "$$(printf '!ys-0:\ntest:: inc(41)')" | \
	  grep -q '"test": 42'

pkg-test: $(HASKELL-DEPS)
	$(MAKE) clean
	$(MAKE) test
	cabal sdist
	cabal exec -- cabal install $(HASKELL-DIST)

ls: dist
	ls -l $(HASKELL-DIST)

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

$(LIBYS-COPY): $(LIBYS-SO-FQNP)
	mkdir -p $(LIBYS)
	$(RM) -r $(LIBYS)/lib
	cp -r ../libys/lib $(LIBYS)/
