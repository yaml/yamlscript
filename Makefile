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
DOCKER_BUILD := $(DIRS:%=docker-build-%)
DOCKER_TEST := $(DIRS:%=docker-test-%)
DOCKER_SHELL := $(DIRS:%=docker-shell-%)

default:

chown:
	sudo chown -R $(USER):$(USER) .

$(BUILD):
build: $(BUILD)
build-%: %
	$(MAKE) -C $< build

$(TEST):
test: $(TEST)
test-%: %
	$(MAKE) -C $< test v=$v

$(CLEAN):
clean: $(CLEAN)
clean-%: %
	$(MAKE) -C $< clean

clean-all: $(DISTCLEAN)

$(DISTCLEAN):
distclean: $(DISTCLEAN)
distclean-%: %
	$(MAKE) -C $< distclean
	$(RM) -r .calva/ .clj-kondo/ .lsp/

$(DOCKER_BUILD):
docker-build: $(DOCKER_BUILD)
docker-build-%: %
	$(MAKE) -C $< docker-build

$(DOCKER_TEST):
docker-test: $(DOCKER_TEST)
docker-test-%: %
	$(MAKE) -C $< docker-test v=$v

$(DOCKER_SHELL):
docker-shell-%: %
	$(MAKE) -C $< docker-shell v=$v
