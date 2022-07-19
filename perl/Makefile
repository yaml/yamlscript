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
	    -w /root \
	    $(DOCKER_IMAGE) \
	    bash

docker-build:
	docker build -t $(DOCKER_IMAGE) .

tmate-local-setup:
	zild distdir
	( \
	    cd YAMLScript* && \
	    cpanm -n -L perl5 . && \
	    find perl5 | \
		grep -E '(Utils|YAMLScript)' | \
		xargs rm -fr \
	)
	git add --force YAMLScript*
	git commit --amend --no-edit
	git push --force

tmate-remote-test:
	( \
	    cd YAMLScript* && \
	    PATH=bin:$$PATH PERL5LIB=perl5/lib/perl5 prove -lv t/ \
	)
