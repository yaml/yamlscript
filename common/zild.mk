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

ifneq (,$(shell command -v zild))
$(ZILD)::
	zild $@
endif
