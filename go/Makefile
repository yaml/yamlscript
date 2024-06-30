include ../common/base.mk
include $(COMMON)/binding.mk

GO_CMDS := \
    build \
    test \

export CGO_CFLAGS := -I $(ROOT)/libyamlscript/lib
export CGO_LDFLAGS := -L $(ROOT)/libyamlscript/lib


$(GO_CMDS)::
	$(make -C $(ROOT)/libyamlscript build
	(cd $(ROOT)/go/ys && go $@)
