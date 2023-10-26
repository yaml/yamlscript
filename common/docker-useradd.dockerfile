
RUN set -x \
 && apt-get install -y sudo \
 && groupadd -g $DOCKER_GID user \
 && useradd -u $DOCKER_UID -g $DOCKER_GID user \
 && adduser user sudo \
 && echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers \
 && mkdir -p /home/user \
 && chown -R user:user /home/user \
 && true

USER user
