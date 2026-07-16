FROM docker.io/dyalog/dyalog

USER root

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      ca-certificates \
      gcc \
      libc6-dev \
      make \
      wget \
      unzip \
      zip \
      xz-utils \
 && rm -rf /var/lib/apt/lists/*

USER dyalog

ENV TATIN_VERSION=0.124.2
ENV TATIN_HOME=/home/dyalog/dyalog.200U64.files/SessionExtensions/CiderTatin
ENV TATIN_URL=https://github.com/aplteam/Tatin/releases/download

RUN mkdir -p ${TATIN_HOME} \
 && wget -q ${TATIN_URL}/v${TATIN_VERSION}/Tatin-Client-${TATIN_VERSION}.zip \
      -O /tmp/tatin-client.zip \
 && unzip -q -o /tmp/tatin-client.zip -d ${TATIN_HOME} \
 && rm /tmp/tatin-client.zip

RUN printf '%s\n' ']activate tatin -reset' ')OFF' \
 | DYALOG_LINEEDITOR_MODE=1 dyalog
