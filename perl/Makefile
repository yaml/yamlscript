include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/zild.mk

test ?= test/

export PATH := $(ROOT)/perl/bin:$(PATH)

PERL_VERSION := $(shell grep '^version:' Meta | cut -d' ' -f2)
ALIEN_DIST_DIR := $(ROOT)/perl-alien/Alien-YAMLScript-$(PERL_VERSION)
ALIEN_BLIB_LIB := $(ALIEN_DIST_DIR)/blib/lib

#------------------------------------------------------------------------------
test::
	prove -l $${TEST_VERBOSE:+'-v'} $(test)

test-alien: $(ALIEN_BLIB_LIB)
	LD_LIBRARY_PATH= \
	    DYLD_LIBRARY_PATH= \
	    prove -I$< -l $${TEST_VERBOSE:+'-v'} $(test)

clean::
	$(RM) -r cpan YAMLScript-*

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
