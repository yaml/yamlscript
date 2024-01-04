include ../common/base.mk
include $(COMMON)/binding.mk

ZILD := \
    cpan \
    cpanshell \
    dist \
    distdir \
    distshell \
    disttest \
    install \
    release \
    update \

test ?= test/

export PATH := $(ROOT)/perl/bin:$(PATH)

PERL_VERSION := $(shell grep '^version:' Meta | cut -d' ' -f2)
ALIEN_DIST_DIR := $(ROOT)/perl-alien/Alien-YAMLScript-$(PERL_VERSION)
ALIEN_BLIB_LIB := $(ALIEN_DIST_DIR)/blib/lib

#------------------------------------------------------------------------------
test:: $(ALIEN_BLIB_LIB)
	prove -I$< -l $${TEST_VERBOSE:+'-v'} $(test)

clean::
	$(RM) -r cpan YAMLScript-*

$(ZILD)::
	zild $@

$(ALIEN_BLIB_LIB): $(ALIEN_DIST_DIR)
	( \
	    cd $< && \
	    perl Makefile.PL && \
	    make \
	)
	touch $@

$(ALIEN_DIST_DIR):
	$(MAKE) -C $(ROOT)/perl-alien distdir

Dockerfile:: $(COMMON) Makefile
	cat \
	  $</docker-from-ubuntu.dockerfile \
	  $</docker-apt-base.dockerfile \
	  $</docker-useradd.dockerfile \
	  $</docker-apt-perl.dockerfile \
	  $</docker-deps-perl.dockerfile \
	  $</docker-apt-dev.dockerfile \
		> $@
