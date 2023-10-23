include ../common/base.mk

ZILD := \
    cpan \
    cpanshell \
    dist \
    distdir \
    distshell \
    disttest \
    install \
    release \
    update \

test ?= test/

export PATH := $(ROOT)/perl/bin:$(PATH)


#------------------------------------------------------------------------------
test::
	prove -l -v $(test)

clean::
	$(RM) -r cpan YAMLScript*

$(ZILD)::
	zild $@
