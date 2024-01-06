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

$(ZILD)::
	zild $@
