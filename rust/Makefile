include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/rust.mk
include $(MAKES)/shell.mk

CARGO-CMDS := \
  build \
  check \
  clippy \
  fmt \
  install \
  publish \
  test \
  update \


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP)

$(CARGO-CMDS):: $(CARGO)
	cargo $@

release: publish

clean::
	$(RM) -r target
