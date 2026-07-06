include ../common/base.mk
include $(COMMON)/binding.mk

# Inside the test container (see test.dockerfile) erlang, elixir and
# hex come from the image:
ifneq (,$(wildcard /.dockerenv))
ELIXIR-DOCKER-INNER := true
export MIX_HOME := /opt/mix
export HEX_HOME := /opt/hex
else
include $(MAKES)/elixir.mk
export MIX_HOME := $(ROOT)/.cache/mix
export HEX_HOME := $(ROOT)/.cache/hex
endif

include $(MAKES)/shell.mk

ELIXIR-IMAGE ?= ghcr.io/yaml/yamlscript-elixir-test


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

# Run the tests in the prebaked container when docker is available
# (locally and on CI Linux runners); erlang never compiles:
ifneq (,$(and $(if $(ELIXIR-DOCKER-INNER),,x),$(shell command -v docker)))
test:: $(LIBYS-SO-FQNP) docker-image
	docker run --rm \
	  --user $(USER-UID):$(USER-GID) \
	  --env HOME=/tmp/home \
	  --volume $(ROOT):$(ROOT) \
	  --workdir $(CURDIR) \
	  $(ELIXIR-IMAGE) \
	  make test

docker-image:
	docker image inspect $(ELIXIR-IMAGE) > /dev/null 2>&1 || \
	docker pull $(ELIXIR-IMAGE) 2> /dev/null || \
	docker build -f test.dockerfile -t $(ELIXIR-IMAGE) .

docker-image-build:
	docker build -f test.dockerfile -t $(ELIXIR-IMAGE) .

docker-image-push: docker-image-build
	docker push $(ELIXIR-IMAGE)

# Publish from the container too (the toolchain path would compile
# Erlang/OTP from source); hex reads HEX_API_KEY from the env:
release: docker-image
	docker run --rm \
	  --user $(USER-UID):$(USER-GID) \
	  --env HOME=/tmp/home \
	  --env HEX_API_KEY \
	  --volume $(ROOT):$(ROOT) \
	  --workdir $(CURDIR) \
	  $(ELIXIR-IMAGE) \
	  make release
else
test:: $(LIBYS-SO-FQNP) $(ERL) $(ELIXIR)
	mix local.hex --force --if-missing
	mix deps.get
	mix test

# Hex requires the file names README.md and a LICENSE:
release: $(ERL) $(ELIXIR) README.md
	mix local.hex --force --if-missing
	mix deps.get
	mix hex.publish --yes
	$(RM) README.md
endif

README.md: ReadMe.md
	cp $< $@

clean::
	$(RM) -r _build deps priv README.md
