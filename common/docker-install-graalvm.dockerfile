
RUN set -x \
 && cd /tmp \
 && curl -o $GRAALVM_TAR \
        https://download.oracle.com/graalvm/21/latest/$GRAALVM_TAR \
 && tar xzf $GRAALVM_TAR \
 && mv graalvm-jdk-21.* graalvm-oracle-21 \
 && true
