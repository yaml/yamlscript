include ../common/base.mk
include ../common/docker.mk

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


#------------------------------------------------------------------------------
test:: $(LIBYAMLSCRIPT_SO_FQNP)
	$(MAKE) -C $(ROOT)/perl-alien distdir
	(cd $(ROOT)/perl-alien/Alien-YAMLScript-* && perl Makefile.PL && make)
	PERL5LIB=$(ROOT)/perl-alien/Alien-YAMLScript-*/blib/lib \
	    prove -l $${TEST_VERBOSE:+'-v'} $(test)

clean::
	$(RM) -r cpan YAMLScript-*

$(ZILD)::
	zild $@

$(LIBYAMLSCRIPT_SO_FQNP): $(ROOT)/libyamlscript
	$(MAKE) -C $< build

Dockerfile:: $(COMMON) Makefile
	cat \
	  $</docker-from-ubuntu.dockerfile \
	  $</docker-apt-base.dockerfile \
	  $</docker-useradd.dockerfile \
	  $</docker-apt-perl.dockerfile \
	  $</docker-deps-perl.dockerfile \
	  $</docker-apt-dev.dockerfile \
		> $@
