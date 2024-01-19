include ../common/base.mk
include $(COMMON)/binding.mk

CARGO := $(shell command -v cargo)

CARGO_CMDS := \
    build \
    clean \
    install \
    publish \
    test \

ifdef CARGO
$(CARGO_CMDS)::
	cargo $@
endif
