include ../common/base.mk
include $(COMMON)/binding.mk

test ?= t/*

export RAKULIB=$(ROOT)/raku/lib

#------------------------------------------------------------------------------
test::
	prove $${TEST_VERBOSE:+'-v'} $(test)

dist::
	fez upload --dry-run

clean::
	$(RM) -r .precomp/ lib/.precomp/ sdist/
