FROM alpine:3.15.0

RUN apk update \
 && apk add \
        bash \
        build-base \
        curl \
        git \
        perl \
        perl-dev \
        perl-app-cpanminus \
        wget \
 && true

RUN cpanm -n \
        Mo \
        Mo::xxx \
        Sub::Name \
        XXX \
        YAML::PP \
 && true
