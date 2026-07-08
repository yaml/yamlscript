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
    FFI::CheckLib \
    FFI::Platypus \
    Test2::V0 \

CPAN-DIST := YAMLScript-$(PERL-VERSION).tar.gz

LOCAL-LIB := local

export PATH := $(ROOT)/perl/$(LOCAL-LIB)/bin:$(PATH)
export PERL5LIB := $(ALIEN-BLIB-LIB):$(LOCAL-LIB)/lib/perl5
export PERL5LIB := $(ROOT)/perl-alien/$(LOCAL-LIB)/lib/perl5:$(PERL5LIB)

ifeq ($(OS-NAME),windows)
PROVE := $(shell command -v prove)
PERL-CMD := $(dir $(PROVE))perl
CPANM-CMD := $(PERL-CMD) -S cpanm
PROVE-CMD := $(PERL-CMD) $(PROVE)
TEST-DEPS := $(PERL) $(LOCAL-LIB)
else
PERL-CMD := perl
CPANM-CMD := cpanm
PROVE-CMD := perl -S prove
TEST-DEPS := $(PERL) $(LOCAL-LIB) $(ALIEN-BLIB-LIB)
endif

#------------------------------------------------------------------------------

ifneq (,$(shell command -v cpanm))
	PERLOK := true
endif

distdir dist:: $(LOCAL-LIB)
	$(ROOT)/util/make-perl-cpan-dist $@

disttest:: $(LOCAL-LIB) $(ALIEN-BLIB-LIB)
	$(ROOT)/util/make-perl-cpan-dist $@

ifdef PERLOK
test:: $(TEST-DEPS)
	$(PROVE-CMD) -l $${TEST_VERBOSE:+'-v'} $(test)

test-alien: $(ALIEN-BLIB-LIB)
	LD_LIBRARY_PATH= \
	    DYLD_LIBRARY_PATH= \
	    $(PROVE-CMD) -I$< -l $${TEST_VERBOSE:+'-v'} $(test)
endif

release: $(LOCAL-LIB) dist
ifndef PAUSE_USER
	$(error 'make release' needs PAUSE_USER set)
endif
ifndef PAUSE_PASS
	$(error 'make release' needs PAUSE_PASS set)
endif
	$(CPANM-CMD) -L $(LOCAL-LIB) -n CPAN::Uploader
	$(ROOT)/util/upload-perl-cpan-dist $(CPAN-DIST)

clean::
	$(RM) -r cpan YAMLScript-*

realclean::
	$(RM) -r $(LOCAL-LIB)

$(ALIEN-BLIB-LIB): $(ALIEN-DIST-DIR)
	( \
	    cd $< && \
	    export PERL5LIB=$(ROOT)/perl-alien/local/lib/perl5:$(PERL5LIB) && \
	    $(PERL-CMD) Makefile.PL && \
	    $(MAKE) \
	)
	touch $@

$(ALIEN-DIST-DIR):
	$(MAKE) -C $(ROOT)/perl-alien distdir

# The $(PERL) prerequisite matters: it guarantees the makes perl
# (whose bin dir ships a cpanm wrapper that always runs its sibling
# perl) is first on PATH before deps are built. Without it, cpanm
# can resolve to a host perl (e.g. perlbrew) and build XS modules
# for the wrong perl version:
$(LOCAL-LIB): Makefile $(PERL)
	$(CPANM-CMD) -L $@ -n $(CPAN-DEPS)
