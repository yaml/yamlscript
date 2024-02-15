include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/docker.mk

export PATH := $(ROOT)/core/bin:$(PATH)

#------------------------------------------------------------------------------
build:: $(YAMLSCRIPT_CORE_INSTALLED)

install test: $(LEIN)
	$< $@

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
