SHELL := bash

ZILD := \
    clean \
    cpan \
    cpanshell \
    dist \
    distdir \
    distshell \
    disttest \
    install \
    release \
    update \

DOCKER_IMAGE := alpine-test-yamlscript-perl

test ?= test/


#------------------------------------------------------------------------------
default:

.PHONY: test
test:
	prove -v $(test)

$(ZILD):
	zild $@

docker-test: docker-build dist
	docker run --rm -it \
	    -v $(PWD):/host \
	    -w /host \
	    $(DOCKER_IMAGE) \
	    bash test/docker-cpan-test.sh

docker-shell: docker-build dist
	touch /tmp/yamlscript-docker-test-history; \
	docker run --rm -it \
	    -v /tmp/yamlscript-docker-test-history:/root/.bash_history \
	    -v $(PWD):/host \
	    -w /host \
	    $(DOCKER_IMAGE) \
	    bash

docker-build:
	docker build -t $(DOCKER_IMAGE) .
