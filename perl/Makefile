include ../common/base.mk
include ../common/docker.mk

ALIEN := $(ROOT)/perl-alien
ALIEN_TAR := $(ALIEN)/share/$(RELEASE_LYS_TAR)

test ?= test/

#------------------------------------------------------------------------------
test:: $(ALIEN_TAR)
	YS_TESTING=1 prove -I$(ALIEN)/lib -l $${TEST_VERBOSE:+'-v'} $(test)

clean::
	$(RM) -r cpan YAMLScript-*
	$(MAKE) -C $(ALIEN) $@

$(ZILD)::
	zild $@

$(ALIEN_TAR): $(ALIEN)
	$(MAKE) -C $< share/$(RELEASE_LYS_TAR)

Dockerfile:: $(COMMON) Makefile
	cat \
	  $</docker-from-ubuntu.dockerfile \
	  $</docker-apt-base.dockerfile \
	  $</docker-useradd.dockerfile \
	  $</docker-apt-perl.dockerfile \
	  $</docker-deps-perl.dockerfile \
	  $</docker-apt-dev.dockerfile \
		> $@
