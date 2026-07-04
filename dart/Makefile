include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/dart.mk
include $(MAKES)/shell.mk

export PUB_CACHE := $(ROOT)/.cache/pub


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(DART)
	dart pub get
	dart run test/main.dart

release: $(DART)
	dart pub publish --force

clean::
	$(RM) -r .dart_tool pubspec.lock
