include ../common/base.mk
include ../common/clojure.mk

export PATH := $(ROOT)/clojure/bin:$(PATH)

DOCKER_IMAGE := yamlscript-clojure:latest
DOCKER_HISTORY := /tmp/yamlscript-clojure-bash-history

#------------------------------------------------------------------------------
build:: $(YAMLSCRIPT_CORE_INSTALLED)

test:
	lein test

clean:: nrepl-stop
	$(RM) .lein-failures .lein-repl-history pom.xml
	$(RM) -r .calva/ .clj-kondo/ .lsp/ .vscode/ .cpcache/ .portal/
	$(RM) -r target/

docker-build:
	docker build --tag $(DOCKER_IMAGE) .

docker-shell: docker-build
	touch $(DOCKER_HISTORY)
	docker run --rm -it \
	    --volume $(ROOT):/host \
	    --volume $(DOCKER_HISTORY):/root/.bash_history \
	    --workdir /host/clojure \
	    --entrypoint /bin/bash \
	    $(DOCKER_IMAGE)
