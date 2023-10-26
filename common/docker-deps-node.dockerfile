
COPY package.json yarn.lock /tmp/

RUN set +x \
 && source $HOME/.nvm/nvm.sh \
 && cd /tmp \
 && yarn install \
 && sudo mv node_modules / \
 && true
