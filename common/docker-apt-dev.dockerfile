
RUN set -x \
 && sudo apt-get install -y \
       bash-completion \
       less \
       locales \
       silversearcher-ag \
       tig \
       tmate \
       tmux \
       vim \
       zsh \
       zsh-common \
 && true
