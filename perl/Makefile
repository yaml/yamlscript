SHELL := bash

PACKAGE_NAME := YAMLScript
PACKAGE_VERSION := 0.0.1
PACKAGE_DIST := $(PACKAGE_NAME)-$(PACKAGE_VERSION).tar.gz

default:

publish: build
	cpan-upload $(PACKAGE_DIST)

build clean::
	dzil $@

test:
	prove -Ilib -v t/
