include ../common/base.mk
include $(COMMON)/binding.mk
include $(MAKES)/raku.mk
include $(MAKES)/shell.mk

test ?= t/*

export RAKULIB=$(ROOT)/raku/lib

# The tests are TAP-emitting raku programs that exit nonzero on
# failure. Run them directly; prove is unreliable on the Windows CI
# runners (a Strawberry Perl script running under the MSYS perl):
test:: $(RAKU)
	@set -e; for t in $(test); do \
	  echo "== $$t"; raku $$t; \
	done

# fez is only needed for publishing; the binding and its tests have
# no module dependencies (zef is also unreliable on Windows):
raku-install: $(RAKU)
	command -v fez > /dev/null || zef install fez


release:
	$(ROOT)/util/release-raku

dist::
	fez upload --dry-run

clean::
	$(RM) -r .precomp/ lib/.precomp/ sdist/
