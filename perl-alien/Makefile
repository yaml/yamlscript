include ../common/base.mk

include $(MAKES)/perl.mk
include $(MAKES)/shell.mk

ZILD-COMMANDS := \
    cpan \
    cpanshell \
    dist \
    distdir \
    distshell \
    disttest \
    install \
    update \

PERL-ALIEN-VERSION := $(shell grep '^version:' Meta | cut -d' ' -f2)
CPAN-DIST := Alien-YAMLScript-$(PERL-ALIEN-VERSION).tar.gz
CPAN-DEPS := \
    CPAN::Uploader \
    Zilla::Dist \

LOCAL-LIB := local

export PATH := $(ROOT)/perl-alien/$(LOCAL-LIB)/bin:$(PATH)
export PERL5LIB := $(LOCAL-LIB)/lib/perl5

#------------------------------------------------------------------------------

$(ZILD-COMMANDS):: $(LOCAL-LIB)
	zild $@

test-cpan:: disttest

release: $(LOCAL-LIB) dist
ifndef PAUSE_USER
	$(error 'make release' needs PAUSE_USER set)
endif
ifndef PAUSE_PASS
	$(error 'make release' needs PAUSE_PASS set)
endif
	cpan-upload --user $(PAUSE_USER) --password '$(PAUSE_PASS)' \
	  $(CPAN-DIST)

clean::
	$(RM) -r cpan Alien-YAMLScript*

build-doc::

$(LOCAL-LIB): $(PERL)
	cpanm -L $@ -n $(CPAN-DEPS)
