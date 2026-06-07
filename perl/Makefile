include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/perl.mk
include $(MAKES)/shell.mk

test ?= test/

export PATH := $(ROOT)/perl/bin:$(PATH)

PERL-VERSION := $(shell grep '^version:' Meta | cut -d' ' -f2)
ALIEN-DIST-DIR := $(ROOT)/perl-alien/Alien-YAMLScript-$(PERL-VERSION)
ALIEN-BLIB-LIB := $(ALIEN-DIST-DIR)/blib/lib

CPAN-DEPS := \
    CPAN::Uploader \
    Cpanel::JSON::XS \
    FFI::CheckLib \
    FFI::Platypus \
    Test2::V0 \

CPAN-DIST := YAMLScript-$(PERL-VERSION).tar.gz

LOCAL-LIB := local

export PATH := $(ROOT)/perl/$(LOCAL-LIB)/bin:$(PATH)
export PERL5LIB := $(ALIEN-BLIB-LIB):$(LOCAL-LIB)/lib/perl5
export PERL5LIB := $(ROOT)/perl-alien/$(LOCAL-LIB)/lib/perl5:$(PERL5LIB)

#------------------------------------------------------------------------------

ifneq (,$(shell command -v cpanm))
	PERLOK := true
endif

distdir dist:: $(LOCAL-LIB)
	$(ROOT)/util/make-perl-cpan-dist $@

disttest:: $(LOCAL-LIB) $(ALIEN-BLIB-LIB)
	$(ROOT)/util/make-perl-cpan-dist $@

ifdef PERLOK
test:: $(LOCAL-LIB)
	prove -l $${TEST_VERBOSE:+'-v'} $(test)

test-alien: $(ALIEN-BLIB-LIB)
	LD_LIBRARY_PATH= \
	    DYLD_LIBRARY_PATH= \
	    prove -I$< -l $${TEST_VERBOSE:+'-v'} $(test)
endif

release: $(LOCAL-LIB) dist
ifndef PAUSE_USER
	$(error 'make release' needs PAUSE_USER set)
endif
ifndef PAUSE_PASS
	$(error 'make release' needs PAUSE_PASS set)
endif
	$(ROOT)/util/upload-perl-cpan-dist $(CPAN-DIST)

clean::
	$(RM) -r cpan YAMLScript-*

realclean::
	$(RM) -r $(LOCAL-LIB)

$(ALIEN-BLIB-LIB): $(ALIEN-DIST-DIR)
	( \
	    cd $< && \
	    export PERL5LIB=$(ROOT)/perl-alien/local/lib/perl5:$(PERL5LIB) && \
	    perl Makefile.PL && \
	    make \
	)
	touch $@

$(ALIEN-DIST-DIR):
	$(MAKE) -C $(ROOT)/perl-alien distdir

$(LOCAL-LIB): Makefile
	cpanm -L $@ -n $(CPAN-DEPS)
