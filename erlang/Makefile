include ../common/base.mk
include $(COMMON)/binding.mk

PERL := /usr/bin/perl
export PERL

include $(MAKES)/erlang.mk
include $(MAKES)/shell.mk

ERLANG-PRIV := priv
ERLANG-NIF := $(ERLANG-PRIV)/yamlscript_nif.$(SO)
ERLANG-INCLUDE := $(ERLANG-LOCAL)/lib/erlang/usr/include

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

ebin/yamlscript.beam: src/yamlscript.erl | ebin
	erlc -o ebin $<

ebin/yamlscript_test.beam: test/yamlscript_test.erl ebin/yamlscript.beam
	erlc -pa ebin -o ebin $<

$(ERLANG-NIF): c_src/yamlscript_nif.c $(ERL) | $(ERLANG-PRIV)
	$(CC) -fPIC $(NIF-LDFLAGS) -I$(ERLANG-INCLUDE) -o $@ $< -ldl

ebin $(ERLANG-PRIV):
	mkdir -p $@

build:: ebin/yamlscript_test.beam

clean::
	$(RM) -r ebin priv
