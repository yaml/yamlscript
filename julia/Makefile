include ../common/base.mk
include $(COMMON)/binding.mk

JULIA := $(shell command -v julia)

HEAD_COMMIT := $(shell git rev-parse HEAD)

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

ifdef JULIA
test:: $(LIBYS_SO_FQNP) deps
	$(JULIA) --project=. test/runtests.jl

deps::
	$(JULIA) --project=. -e 'using Pkg; Pkg.add("JSON")'

release:
ifndef YS_GH_TOKEN
	$(error YS_GH_TOKEN not found)
endif
	curl -L -X POST \
	  -H "Accept: application/vnd.github+json" \
	  -H "Authorization: Bearer $(YS_GH_TOKEN)" \
	  -H "X-GitHub-Api-Version: 2022-11-28" \
	  $(YS_GH_API_URL)/commits/$(HEAD_COMMIT)/comments \
	  -d '{"body":"@JuliaRegistrator register subdir=julia\n"}'

repl:
	$(JULIA) --project=.
endif

clean::
	$(RM) Manifest.toml
