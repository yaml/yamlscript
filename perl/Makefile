include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/zild.mk

test ?= test/

export PATH := $(ROOT)/perl/bin:$(PATH)

PERL_VERSION := $(shell grep '^version:' Meta | cut -d' ' -f2)
ALIEN_DIST_DIR := $(ROOT)/perl-alien/Alien-YAMLScript-$(PERL_VERSION)
ALIEN_BLIB_LIB := $(ALIEN_DIST_DIR)/blib/lib

ifneq (,$(shell command -v cpanm))
	PERLOK := true
endif

CPAN_DEPS := \
    Test2::V0 \
    FFI::CheckLib \
    FFI::Platypus \
    Cpanel::JSON::XS \

LOCAL_LIB := local

export PERL5LIB := $(LOCAL_LIB)/lib/perl5

#------------------------------------------------------------------------------

ifdef PERLOK
test:: $(LOCAL_LIB)
	prove -l $${TEST_VERBOSE:+'-v'} $(test)

test-alien: $(ALIEN_BLIB_LIB)
	LD_LIBRARY_PATH= \
	    DYLD_LIBRARY_PATH= \
	    prove -I$< -l $${TEST_VERBOSE:+'-v'} $(test)
endif

clean::
	$(RM) -r cpan YAMLScript-*

realclean::
	$(RM) -r $(LOCAL_LIB)

$(ALIEN_BLIB_LIB): $(ALIEN_DIST_DIR)
	( \
	    cd $< && \
	    perl Makefile.PL && \
	    make \
	)
	touch $@

$(ALIEN_DIST_DIR):
	$(MAKE) -C $(ROOT)/perl-alien distdir

$(LOCAL_LIB):
	cpanm -L $@ -n $(CPAN_DEPS)
