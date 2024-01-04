include ../common/base.mk

CARGO := \
    build \
    clean \
    install \
    publish \
    test \

default::

ifneq (,$(shell command -v cargo))
$(CARGO)::
	cargo $@
endif
