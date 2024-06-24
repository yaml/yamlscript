include ../common/base.mk
include $(COMMON)/binding.mk

CARGO := $(shell command -v cargo)

CARGO_CMDS := \
    build \
    clean \
    install \
    publish \
    test \


build:: ReadMe.md

ifdef CARGO
$(CARGO_CMDS)::
	cargo $@

release: publish
endif
