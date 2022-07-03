SHELL := bash

default:

test: test-perl

test-%: %
	$(MAKE) -C $< test
