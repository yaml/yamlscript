RUN apt-get update && apt-get install -y \
        bash-completion \
        curl \
        gcc \
        git \
        libz-dev \
        locales \
        make \
        xz-utils \
 && echo 'en_US.UTF-8 UTF-8' >> /etc/locale.gen \
 && locale-gen en_US.UTF-8 \
 && true

ENV LANG en_US.UTF-8
ENV LC_ALL en_US.UTF-8

RUN apt-get install -y \
        less \
        tig \
        vim \
 && true
