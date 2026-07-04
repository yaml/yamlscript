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

# pub.dev requires the file names README.md and CHANGELOG.md:
release: $(DART) README.md CHANGELOG.md
	dart pub publish --force
	$(RM) README.md CHANGELOG.md

README.md: ReadMe.md
	cp $< $@

CHANGELOG.md:
	printf '## %s\n\nSee the YAMLScript change log:\n%s\n' \
	  "$(YAMLSCRIPT_VERSION)" \
	  "https://github.com/yaml/yamlscript/blob/main/Changes" > $@

clean::
	$(RM) -r .dart_tool pubspec.lock README.md CHANGELOG.md
