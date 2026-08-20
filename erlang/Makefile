include ../common/base.mk
include $(COMMON)/binding.mk

PERL := /usr/bin/perl
export PERL

ifneq (,$(wildcard /.dockerenv))
ERLANG-DOCKER-INNER := true
ERL := erl
ERLC := erlc
REBAR3 := rebar3
ERLANG-DEPS :=
ERLANG-ROOT := $(shell erl -noshell \
  -eval 'io:format("~s", [code:root_dir()]).' -s init stop)
ERLANG-INCLUDE := $(ERLANG-ROOT)/usr/include
else
include $(MAKES)/erlang.mk
include $(MAKES)/rebar3.mk
ERLANG-DEPS := $(ERL)
ERLANG-INCLUDE := $(ERLANG-LOCAL)/lib/erlang/usr/include
endif

include $(MAKES)/shell.mk

export REBAR_CACHE_DIR := $(ROOT)/.cache/rebar3

HEX-API-KEY ?= $(HEX_API_KEY)
ifeq (,$(HEX-API-KEY))
HEX-API-KEY := \
  $(shell ys -e '.hex.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif

ERLANG-PRIV := priv
ERLANG-NIF := $(ERLANG-PRIV)/yamlscript_nif.$(SO)
NIF-CFLAGS := -DYAMLSCRIPT_VERSION=\"$(YAMLSCRIPT_VERSION)\"
ERLANG-IMAGE ?= ghcr.io/yaml/yamlscript-elixir-test
ERLANG-DOCKER-OUTER := $(and \
  $(IS-LINUX), \
  $(IS-INTEL), \
  $(if $(ERLANG-DOCKER-INNER),,x), \
  $(shell command -v docker))

ifeq ($(OS-NAME),macos)
NIF-LDFLAGS := -dynamiclib
else
NIF-LDFLAGS := -shared
endif


#------------------------------------------------------------------------------
build:: build-doc $(ERLANG-NIF) ebin/yamlscript.beam

build-doc:: ReadMe.md

ifneq (,$(ERLANG-DOCKER-OUTER))
test:: $(LIBYS-SO-FQNP) docker-image
	docker run --rm \
	  --user $(USER-UID):$(USER-GID) \
	  --env HOME=/tmp/home \
	  --volume $(ROOT):$(ROOT) \
	  --workdir $(CURDIR) \
	  $(ERLANG-IMAGE) \
	  make test

docker-image:
	docker image inspect $(ERLANG-IMAGE) > /dev/null 2>&1 || \
	docker pull $(ERLANG-IMAGE) 2> /dev/null || \
	$(MAKE) -C $(ROOT)/elixir docker-image
else
test:: $(LIBYS-SO-FQNP) $(ERLANG-DEPS) build
	erl -noshell -pa ebin -eval 'yamlscript_test:run().' -s init stop
endif

ifneq (,$(ERLANG-DOCKER-OUTER))
release: export HEX_API_KEY := $(HEX-API-KEY)
release: docker-image
	docker run --rm \
	  --user $(USER-UID):$(USER-GID) \
	  --env HOME=/tmp/home \
	  --env HEX_API_KEY \
	  --volume $(ROOT):$(ROOT) \
	  --workdir $(CURDIR) \
	  $(ERLANG-IMAGE) \
	  make release
else
release: build-doc
ifndef HEX-API-KEY
	$(error Can't determine Hex API key)
endif
	@command -v $(REBAR3) >/dev/null || \
	  { echo "'$(REBAR3)' is required to publish the Erlang binding" >&2; \
	    exit 2; }
	HEX_API_KEY=$(HEX-API-KEY) $(REBAR3) \
	  hex publish package --repo hexpm --yes
endif

ebin/yamlscript.beam: src/yamlscript.erl | ebin
	erlc -o ebin $<

ebin/yamlscript_test.beam: test/yamlscript_test.erl ebin/yamlscript.beam
	erlc -pa ebin -o ebin $<

$(ERLANG-NIF): c_src/yamlscript_nif.c $(ERLANG-DEPS) | $(ERLANG-PRIV)
	$(CC) -fPIC $(NIF-LDFLAGS) $(NIF-CFLAGS) \
	  -I$(ERLANG-INCLUDE) -o $@ $< -ldl

ebin $(ERLANG-PRIV):
	mkdir -p $@

build:: ebin/yamlscript_test.beam

clean::
	$(RM) -r _build ebin priv
	$(RM) rebar.lock
