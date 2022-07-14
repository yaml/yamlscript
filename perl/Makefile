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
	    bash -c ' \
		( \
		    set -x && ( \
			tar xzf YAMLScript*.tar.gz && \
			rm YAMLScript*.tar.gz && \
			cd YAMLScript* && \
			ls -l t/ && \
			perl Makefile.PL && \
			make test \
		    ) || true; \
		    rm -fr YAMLScript* \
		) \
		'

docker-shell: docker-build dist
	docker run --rm -it \
	    -v $(PWD):/host \
	    -w /host \
	    $(DOCKER_IMAGE) \
	    bash

docker-build:
	docker build -t $(DOCKER_IMAGE) .
