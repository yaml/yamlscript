SHELL := bash

LANG := \
    perl \
    python \
    js \

BUILD := $(LANG:%=build-%)
TEST := $(LANG:%=test-%)
PUBLISH := $(LANG:%=publish-%)
CLEAN := $(LANG:%=clean-%)

default:

build: $(BUILD)
build-%: %
	$(MAKE) -C $< build

test: $(TEST)
test-%: %
	$(MAKE) -C $< test

clean: $(CLEAN)
clean-%: %
	$(MAKE) -C $< clean
