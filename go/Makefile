include ../common/base.mk
include $(COMMON)/binding.mk
include $(MAKES)/go.mk
include $(MAKES)/shell.mk


#------------------------------------------------------------------------------
# Go vars:
#------------------------------------------------------------------------------

ifeq ($(OS-NAME),windows)
# go.exe spawns gcc directly (no MSYS conversion), so the flags
# need Windows-style paths:
export CGO_CFLAGS := -I $(shell cygpath -m $(ROOT)/libys/lib)
export CGO_LDFLAGS := -L $(shell cygpath -m $(ROOT)/libys/lib)
else
export CGO_CFLAGS := -I $(ROOT)/libys/lib
export CGO_LDFLAGS := -L $(ROOT)/libys/lib
endif

unexport GOROOT GOPATH GOBIN GOMODCACHE
export GOROOT := $(GO-LOCAL)
export GOPATH := $(ROOT)/go/go
export GOBIN := $(GOROOT)/bin

export PATH := $(GOBIN):$(PATH)


#------------------------------------------------------------------------------
# In installed-artifacts CI mode skip the doc build: build-doc has
# a ys prerequisite whose unix-style ROOT path native Windows make
# cannot stat (ReadMe.md is tracked and current anyway):
ifdef YS_RELEASE_USE_INSTALLED_YS
build:: $(LIBYS-SO-FQNP)
else
build:: build-doc $(LIBYS-SO-FQNP)
endif

build-doc:: ReadMe.md

test:: build $(GO)
	go $@
	[[ ! -d $(GOPATH) ]] || \
	  chmod -R u+w $(GOPATH)

release:
	$(ROOT)/util/release-go

clean::
	[[ ! -d $(GOPATH) ]] || \
	  chmod -R u+w $(GOPATH)
	$(RM) -r $(GOPATH)

distclean:: clean
	$(RM) -r $(LOCAL-CACHE)/go*


#------------------------------------------------------------------------------
pseudo-version:
	@TZ=UTC git --no-pager show \
	   --quiet \
	   --abbrev=12 \
	   --date=format-local:%Y%m%d%H%M%S \
	   --format=v0.0.0-%cd-%h
