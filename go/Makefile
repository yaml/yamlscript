include ../common/base.mk
include $(COMMON)/binding.mk


#------------------------------------------------------------------------------
# Go vars:
#------------------------------------------------------------------------------

GO_VER := 1.22.4

ifdef IS_LINUX
  ifdef IS_INTEL
    GO_ARCH := linux-amd64
  else ifdef IS_ARM
    GO_ARCH := linux-arm64
  endif
else ifdef MACOS
  ifdef IS_INTEL
    GO_ARCH := darwin-amd64
  else ifdef IS_ARM
    GO_ARCH := darwin-arm64
  endif
endif

GO_DIR := go$(GO_VER)
GO_TAR := $(GO_DIR).$(GO_ARCH).tar.gz
GO_TARBALL := $(YS_TMP)/$(GO_TAR)
GO_URL := https://go.dev/dl/$(GO_TAR)
GO_ROOT := $(YS_TMP)/go$(GO_VER)
GO_BIN := $(GO_ROOT)/bin
GO_INSTALLED := $(GO_BIN)/go

export CGO_CFLAGS := -I $(ROOT)/libyamlscript/lib
export CGO_LDFLAGS := -L $(ROOT)/libyamlscript/lib

unexport GOROOT GOPATH GOBIN GOMODCACHE
export GOROOT := $(GO_ROOT)
export GOPATH := $(ROOT)/go/go
export GOBIN := $(GOROOT)/bin

export PATH := $(GOBIN):$(PATH)


#------------------------------------------------------------------------------
build:: build-doc $(LIBYAMLSCRIPT_SO_FQNP) $(GO_INSTALLED)

build-doc:: ReadMe.md

test:: build
	go $@
	chmod -R u+w $(GOPATH)

release:
	$(ROOT)/util/release-go

clean::
	[[ ! -d $(GOPATH) ]] || \
	  chmod -R u+w $(GOPATH)
	$(RM) -r $(GOPATH)

sysclean:: clean
	$(RM) -r $(YS_TMP)/go*


#------------------------------------------------------------------------------
pseudo-version:
	@TZ=UTC git --no-pager show \
	   --quiet \
	   --abbrev=12 \
	   --date=format-local:%Y%m%d%H%M%S \
	   --format=v0.0.0-%cd-%h

print-goroot: $(GO_INSTALLED)
	@echo $(GOROOT)


#------------------------------------------------------------------------------
$(GO_INSTALLED): $(GO_TARBALL)
	mkdir -p $(YS_TMP)
	tar xf $< -C $(YS_TMP)
	mv $(YS_TMP)/go $(YS_TMP)/$(GO_DIR)
	touch $@

$(GO_TARBALL):
	curl -sL -o $@ $(GO_URL)
