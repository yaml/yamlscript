RUN apt-get update && apt-get install -y \
        bash-completion \
        curl \
        gcc \
        git \
        libz-dev \
        make \
        xz-utils \
 && true

 RUN apt-get install -y \
        less \
        tig \
        vim \
 && true
