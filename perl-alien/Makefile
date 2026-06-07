include ../common/base.mk

include $(MAKES)/perl.mk
include $(MAKES)/shell.mk

PERL-ALIEN-VERSION := $(shell grep '^version:' Meta | cut -d' ' -f2)
CPAN-DIST := Alien-YAMLScript-$(PERL-ALIEN-VERSION).tar.gz
CPAN-DEPS := \
    Alien::Build \
    Alien::Build::MM \
    Alien::Build::Plugin::Download::GitHub \
    CPAN::Uploader \
    Cpanel::JSON::XS \
    Mozilla::CA \
    Test2::V0 \
    Test::Alien \

LOCAL-LIB := local

export PATH := $(ROOT)/perl-alien/$(LOCAL-LIB)/bin:$(PATH)
export PERL5LIB := $(LOCAL-LIB)/lib/perl5

#------------------------------------------------------------------------------

distdir dist disttest:: $(LOCAL-LIB)
	$(ROOT)/util/make-perl-cpan-dist $@

test-cpan:: disttest

release: $(LOCAL-LIB) dist
ifndef PAUSE_USER
	$(error 'make release' needs PAUSE_USER set)
endif
ifndef PAUSE_PASS
	$(error 'make release' needs PAUSE_PASS set)
endif
	$(ROOT)/util/upload-perl-cpan-dist $(CPAN-DIST)

clean::
	$(RM) -r cpan Alien-YAMLScript*

realclean::
	$(RM) -r $(LOCAL-LIB)

build-doc::

$(LOCAL-LIB): $(PERL) Makefile
	cpanm -L $@ -n $(CPAN-DEPS)
