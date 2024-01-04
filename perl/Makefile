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
	( \
	    cd "$$(echo "$(ROOT)/perl-alien/Alien-YAMLScript-"*)" && \
	    pwd && \
	    perl Makefile.PL && \
	    make \
	)
	prove -I"$$(echo "$(ROOT)/perl-alien/Alien-YAMLScript-"*)/blib/lib" \
	    -l $${TEST_VERBOSE:+'-v'} $(test)
	PERL5LIB=$(ROOT)/perl-alien/Alien-YAMLScript-*/blib/lib \

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
