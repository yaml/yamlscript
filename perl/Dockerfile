FROM alpine:3.15.0

RUN apk update \
 && apk add \
        bash \
        bash-completion \
        build-base \
        curl \
        git \
        perl \
        perl-dev \
        perl-app-cpanminus \
        tig \
        tmux \
        vim \
        wget \
 && true

ENV SHELL=bash

RUN curl -L https://install.perlbrew.pl | bash \
 && time /root/perl5/perlbrew/bin/perlbrew install -j 5 --notest perl-5.36.0 \
 && true

RUN touch /root/.bashrc \
 && ( \
        echo 'source /etc/profile.d/bash_completion.sh'; \
        echo 'source /root/perl5/perlbrew/etc/bashrc'; \
        echo 'PS1="\s-\v \w \$ "'; \
        echo 'alias gst="(set -x; git status --ignored)"'; \
        echo 'alias gcn="(set -x; git clean -dxn)"'; \
        echo 'alias gcf="(set -x; git clean -dxf)"'; \
        echo 'alias ll="ls -l"'; \
        echo 'alias pmf="perl Makefile.PL"'; \
        echo 'alias mt="make test"'; \
        echo 'alias pb=perlbrew'; \
        echo 'alias pbi="perlbrew install-cpanm"'; \
        echo 'alias pbu="perlbrew use $(perlbrew list)"'; \
    ) >> /root/.bashrc \
 && echo 'source /root/.bashrc' >> /root/profile \
 && true
