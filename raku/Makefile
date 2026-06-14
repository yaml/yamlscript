include ../common/base.mk
include $(COMMON)/binding.mk
include $(MAKES)/raku.mk
include $(MAKES)/shell.mk

test ?= t/*

export RAKULIB=$(ROOT)/raku/lib

FEZ-UPLOAD = fez upload

ifdef YS_RELEASE_CI
FEZ-UPLOAD = timeout 60s fez upload --unattended --force || \
	echo 'Ignoring failed or timed out Raku fez upload'
endif



test:: raku-install
	prove $${TEST_VERBOSE:+'-v'} $(test)

raku-install: $(RAKU)
ifeq (,$(shell find $(LOCAL-TMP) | grep -F 'LibraryMake.rakumod'))
	zef install LibraryMake fez
endif


release: raku-install
	$(FEZ-UPLOAD)

dist::
	fez upload --dry-run

clean::
	$(RM) -r .precomp/ lib/.precomp/ sdist/
