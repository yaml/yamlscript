# Test image for the Haskell binding.
#
# Bakes GHC, cabal, the Hackage index and the compiled dependency
# tree of the yamlscript package into image layers, so running the
# tests never has to install a toolchain or compile dependencies
# (the aeson tree alone takes many minutes on CI runners).
#
# Uses the same GHC and cabal bindists as the makes ghc.mk/cabal.mk
# files. GHC profiling libraries and docs are stripped (tests build
# only the default way). The uncompressed Hackage 01-index.tar must
# stay: the cabal solver fails without it.
#
# Rebuild and push when the dependencies in cabal.ys change:
#   make -C haskell docker-image-build docker-image-push

FROM ubuntu:latest

ARG GHC_VERSION=9.12.1
ARG CABAL_VERSION=3.14.2.0

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      ca-certificates curl xz-utils make gcc g++ binutils \
      binutils-gold libc6-dev libgmp-dev libtinfo6 libnuma1 \
      zlib1g-dev && \
    rm -rf /var/lib/apt/lists/*

# GHC bindist (relocatable; extracted the same way ghc.mk does),
# minus profiling libraries and documentation:
RUN curl -sSL "https://downloads.haskell.org/ghc/$GHC_VERSION/ghc-$GHC_VERSION-x86_64-ubuntu22_04-linux.tar.xz" | \
      tar -xJ -C /opt && \
    mv /opt/ghc-$GHC_VERSION-* /opt/ghc && \
    find /opt/ghc \( -name '*_p.a' -o -name '*.p_hi' \) -delete && \
    rm -rf /opt/ghc/share/doc

# cabal-install binary:
RUN curl -sSL "https://downloads.haskell.org/~cabal/cabal-install-$CABAL_VERSION/cabal-install-$CABAL_VERSION-x86_64-linux-ubuntu22_04.tar.xz" | \
      tar -xJ -C /usr/local/bin cabal

ENV PATH=/opt/ghc/bin:$PATH

# Keep the cabal state in a world-writable, non-HOME location so the
# container can run as any uid:
ENV CABAL_DIR=/opt/cabal

COPY yamlscript.cabal /deps/yamlscript.cabal

RUN mkdir -p /deps/lib /deps/test /tmp/libys/lib && \
    cd /deps && \
    cabal update && \
    cabal build --only-dependencies --enable-tests \
      --enable-benchmarks && \
    chmod -R a+rwX /opt/cabal /tmp/libys
