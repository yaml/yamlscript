
RUN set -x \
 && sudo apt-get install -y \
        build-essential \
        curl \
        python3 \
        python3-pytest \
        python3-venv \
 && true
