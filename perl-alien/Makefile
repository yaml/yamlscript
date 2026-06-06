include ../common/base.mk

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
CPAN-UPLOAD := $(shell command -v cpan-upload)
CPAN-DIST := Alien-YAMLScript-$(PERL-ALIEN-VERSION).tar.gz

#------------------------------------------------------------------------------

ifneq (,$(shell command -v zild))
$(ZILD-COMMANDS)::
	zild $@
endif

test-cpan:: disttest

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
	$(RM) -r cpan Alien-YAMLScript*

build-doc::
