include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/shell.mk

test ?= test/

ZILD-COMMANDS := \
    cpan \
    cpanshell \
    dist \
    distdir \
    distshell \
    disttest \
    install \
    update \

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

CPAN-UPLOAD := $(shell command -v cpan-upload)
CPAN-DIST := YAMLScript-$(PERL-VERSION).tar.gz

LOCAL-LIB := local

export PERL5LIB := $(LOCAL-LIB)/lib/perl5

#------------------------------------------------------------------------------

ifneq (,$(shell command -v cpanm))
	PERLOK := true
endif

ifneq (,$(shell command -v zild))
$(ZILD-COMMANDS)::
	zild $@
endif

ifdef PERLOK
test:: $(LOCAL-LIB)
	prove -l $${TEST_VERBOSE:+'-v'} $(test)

test-alien: $(ALIEN-BLIB-LIB)
	LD_LIBRARY_PATH= \
	    DYLD_LIBRARY_PATH= \
	    prove -I$< -l $${TEST_VERBOSE:+'-v'} $(test)
endif

release: dist
ifndef PAUSE_USER
	$(error 'make release' needs PAUSE_USER set)
endif
ifndef PAUSE_PASS
	$(error 'make release' needs PAUSE_PASS set)
endif
ifndef CPAN-UPLOAD
	$(error 'make release' needs cpan-upload installed)
endif
	cpan-upload --user $(PAUSE_USER) --password '$(PAUSE_PASS)' \
	  $(CPAN-DIST)

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
