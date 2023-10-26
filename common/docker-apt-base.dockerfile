
RUN set -x \
 && apt-get update \
 && apt-get install -y \
        apt-file \
        apt-transport-https \
        autoconf \
        automake \
        bash \
        build-essential \
        cpio \
        curl \
        dialog \
        git \
        iputils-ping \
        libtool \
        pkgconf \
        unzip \
        wget \
        zip \
        zlib1g-dev \
 && true
