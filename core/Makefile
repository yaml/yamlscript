include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/docker.mk

export PATH := $(ROOT)/core/bin:$(PATH)

#------------------------------------------------------------------------------
build:: $(RAPIDYAML_SO) $(RAPIDYAML_INSTALLED)

install test:: $(LEIN) build
	$< $@

$(RAPIDYAML_SO):
	$(MAKE) -C $(ROOT)/rapidyaml build

$(RAPIDYAML_INSTALLED):
	$(MAKE) -C $(ROOT)/rapidyaml install

Dockerfile:: $(COMMON) Makefile
	cat \
	  $</docker-from-ubuntu.dockerfile \
	  $</docker-apt-base.dockerfile \
	  $</docker-useradd.dockerfile \
	  $</docker-apt-clojure.dockerfile \
	  $</docker-copy-project-clj.dockerfile \
	  $</docker-deps-clojure.dockerfile \
	  $</docker-apt-dev.dockerfile \
	  > $@

clean::
	$(RM) pom.xml
	$(RM) -r target/
