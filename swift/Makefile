include ../common/base.mk
include $(COMMON)/binding.mk

SWIFT-VERSION := 6.3.3

include $(MAKES)/swift.mk
include $(MAKES)/shell.mk

SWIFT-FLAGS := --cache-path $(ROOT)/.cache/swiftpm


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(SWIFT)
	swift test $(SWIFT-FLAGS)

release:
	$(ROOT)/util/release-swift

clean::
	$(RM) -r .build
