include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/zild.mk

test ?= test/

export PATH := $(ROOT)/perl/bin:$(PATH)

PERL-VERSION := $(shell grep '^version:' Meta | cut -d' ' -f2)
ALIEN-DIST-DIR := $(ROOT)/perl-alien/Alien-YAMLScript-$(PERL-VERSION)
ALIEN-BLIB-LIB := $(ALIEN-DIST-DIR)/blib/lib

CPAN-DEPS := \
    Cpanel::JSON::XS \
    FFI::CheckLib \
    FFI::Platypus \
    Test2::V0 \

LOCAL-LIB := local

export PERL5LIB := $(LOCAL-LIB)/lib/perl5

#------------------------------------------------------------------------------

ifdef PERLOK
test:: $(LOCAL-LIB)
	prove -l $${TEST_VERBOSE:+'-v'} $(test)

test-alien: $(ALIEN-BLIB-LIB)
	LD_LIBRARY_PATH= \
	    DYLD_LIBRARY_PATH= \
	    prove -I$< -l $${TEST_VERBOSE:+'-v'} $(test)
endif

clean::
	$(RM) -r cpan YAMLScript-*

realclean::
	$(RM) -r $(LOCAL-LIB)

$(ALIEN-BLIB-LIB): $(ALIEN-DIST-DIR)
	( \
	    cd $< && \
	    perl Makefile.PL && \
	    make \
	)
	touch $@

$(ALIEN-DIST-DIR):
	$(MAKE) -C $(ROOT)/perl-alien distdir

$(LOCAL-LIB):
	cpanm -L $@ -n $(CPAN-DEPS)
