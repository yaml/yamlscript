include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/zig.mk
include $(MAKES)/shell.mk

export ZIG_GLOBAL_CACHE_DIR := $(ROOT)/.cache/zig


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(ZIG)
	zig build test

release:
	$(ROOT)/util/release-zig

clean::
	$(RM) -r .zig-cache zig-out
