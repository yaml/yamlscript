include ../common/base.mk
include $(COMMON)/binding.mk

PERL := /usr/bin/perl
export PERL

include $(MAKES)/erlang.mk
include $(MAKES)/rebar3.mk
include $(MAKES)/shell.mk

export REBAR_CACHE_DIR := $(ROOT)/.cache/rebar3

HEX-API-KEY ?= $(HEX_API_KEY)
ifeq (,$(HEX-API-KEY))
HEX-API-KEY := \
  $(shell ys -e '.hex.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif

ERLANG-PRIV := priv
ERLANG-NIF := $(ERLANG-PRIV)/yamlscript_nif.$(SO)
ERLANG-INCLUDE := $(ERLANG-LOCAL)/lib/erlang/usr/include
NIF-CFLAGS := -DYAMLSCRIPT_VERSION=\"$(YAMLSCRIPT_VERSION)\"

ifeq ($(OS-NAME),macos)
NIF-LDFLAGS := -dynamiclib
else
NIF-LDFLAGS := -shared
endif


#------------------------------------------------------------------------------
build:: build-doc $(ERLANG-NIF) ebin/yamlscript.beam

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(ERL) build
	erl -noshell -pa ebin -eval 'yamlscript_test:run().' -s init stop

release: $(REBAR3) build-doc
ifndef HEX-API-KEY
	$(error Can't determine Hex API key)
endif
	HEX_API_KEY=$(HEX-API-KEY) $(REBAR3) \
	  hex publish package --repo hexpm --yes

ebin/yamlscript.beam: src/yamlscript.erl | ebin
	erlc -o ebin $<

ebin/yamlscript_test.beam: test/yamlscript_test.erl ebin/yamlscript.beam
	erlc -pa ebin -o ebin $<

$(ERLANG-NIF): c_src/yamlscript_nif.c $(ERL) | $(ERLANG-PRIV)
	$(CC) -fPIC $(NIF-LDFLAGS) $(NIF-CFLAGS) \
	  -I$(ERLANG-INCLUDE) -o $@ $< -ldl

ebin $(ERLANG-PRIV):
	mkdir -p $@

build:: ebin/yamlscript_test.beam

clean::
	$(RM) -r ebin priv
