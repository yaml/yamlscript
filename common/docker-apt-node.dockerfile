
RUN set +x \
 && curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh \
        | bash \
 && export NVM_DIR=$HOME/.nvm \
 && ls -la $HOME \
 && source "$NVM_DIR/nvm.sh" \
 && nvm install --lts \
 && npm install -g yarn \
 && echo "source $NVM_DIR/nvm.sh" >> $HOME/.bashrc \
 && echo "source $HOME/.bashrc" >> $HOME/.bash_profile \
 && true
