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

#------------------------------------------------------------------------------
clean::
	$(RM) -r cpan Alien-YAMLScript*

$(ZILD)::
	zild $@
