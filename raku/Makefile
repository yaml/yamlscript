include ../common/base.mk
# include ../common/docker.mk

test ?= t/*

export RAKULIB=$(ROOT)/raku/lib

#------------------------------------------------------------------------------
test:: $(LIBYAMLSCRIPT_SO_FQNP)
	prove $${TEST_VERBOSE:+'-v'} $(test)

clean::
	$(RM) -r lib/.precomp/
