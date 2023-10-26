include ../common/base.mk
include ../common/clojure.mk
include ../common/docker.mk

export PATH := $(ROOT)/clojure/bin:$(PATH)

#------------------------------------------------------------------------------
build:: $(YAMLSCRIPT_CORE_INSTALLED)

test:
	lein test

clean:: nrepl-stop
	$(RM) .lein-failures .lein-repl-history pom.xml
	$(RM) -r .calva/ .clj-kondo/ .lsp/ .vscode/ .cpcache/ .portal/
	$(RM) -r target/

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
