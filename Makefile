SHELL := bash

DIRS := \
    cli \
    clojure \
    libyamlscript \
    perl \
    python \

BUILD := $(DIRS:%=build-%)
TEST := $(DIRS:%=test-%)
PUBLISH := $(DIRS:%=publish-%)
CLEAN := $(DIRS:%=clean-%)
DISTCLEAN := $(DIRS:%=distclean-%)

default:

$(BUILD):
build: $(BUILD)
build-%: %
	$(MAKE) -C $< build

$(TEST):
test: $(TEST)
test-%: %
	$(MAKE) -C $< test

$(CLEAN):
clean: $(CLEAN)
clean-%: %
	$(MAKE) -C $< clean

$(DISTCLEAN):
distclean: $(DISTCLEAN)
distclean-%: %
	$(MAKE) -C $< distclean
	$(RM) -r .calva/ .clj-kondo/ .lsp/
