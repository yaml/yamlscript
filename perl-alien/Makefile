include ../common/base.mk
include ../common/docker.mk

test ?= test/

#------------------------------------------------------------------------------
clean::
	$(RM) -r cpan share Alien-YAMLScript*

$(ZILD)::
	zild $@

share/$(RELEASE_LYS_TAR):
	make -C $(ROOT) $(@:share/%=%)
	mkdir -p share
	mv $(ROOT)/$(@:share/%=%) $@

Dockerfile:: $(COMMON) Makefile
	cat \
	  $</docker-from-ubuntu.dockerfile \
	  $</docker-apt-base.dockerfile \
	  $</docker-useradd.dockerfile \
	  $</docker-apt-perl.dockerfile \
	  $</docker-deps-perl.dockerfile \
	  $</docker-apt-dev.dockerfile \
		> $@
