ZILD_COMMANDS := \
    cpan \
    cpanshell \
    dist \
    distdir \
    distshell \
    disttest \
    install \
    release \
    update \

ifneq (,$(shell command -v cpanm))
	PERLOK := true
endif

ifneq (,$(shell command -v zild))
$(ZILD_COMMANDS)::
	zild $@
endif
