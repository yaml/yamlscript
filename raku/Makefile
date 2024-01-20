include ../common/base.mk
include $(COMMON)/binding.mk

test ?= t/*

export RAKULIB=$(ROOT)/raku/lib

RAKU := $(shell command -v raku)

#------------------------------------------------------------------------------
test::
ifdef RAKU
	prove $${TEST_VERBOSE:+'-v'} $(test)
endif

release:
	fez upload

dist::
	fez upload --dry-run

clean::
	$(RM) -r .precomp/ lib/.precomp/ sdist/
