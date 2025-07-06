include ../common/base.mk
include $(COMMON)/binding.mk

RAKU-VERSION := 2025.06.1-01
include $(MAKES)/raku.mk
include $(MAKES)/shell.mk

test ?= t/*

export RAKULIB=$(ROOT)/raku/lib



test:: raku-install
	prove $${TEST_VERBOSE:+'-v'} $(test)

raku-install: $(RAKU)
ifeq (,$(shell find $(LOCAL-TMP) | grep -F 'LibraryMake.rakumod'))
	zef install LibraryMake fez
endif


release:
	fez upload

dist::
	fez upload --dry-run

clean::
	$(RM) -r .precomp/ lib/.precomp/ sdist/
