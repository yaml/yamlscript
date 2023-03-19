SHELL := bash

ROOT := $(shell pwd -P)

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

test ?= test/

export PATH := $(ROOT)/bin:$(PATH)


#------------------------------------------------------------------------------
default:

.PHONY: test
test:
	prove -l -v $(test)

$(ZILD):
	zild $@
