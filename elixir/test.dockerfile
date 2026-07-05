# Test image for the Elixir binding.
#
# The makes toolchain compiles Erlang/OTP from source, which takes
# many minutes on CI runners. This image uses the official elixir
# image (same Elixir and OTP versions as the makes elixir.mk and
# erlang.mk files) with hex preinstalled and the dependency cache
# primed, so running the tests never has to build a toolchain.
#
# Rebuild and push when the toolchain or dependency versions change:
#   make -C elixir docker-image-build docker-image-push

FROM elixir:1.20-otp-28

# Keep the mix and hex state in world-writable, non-HOME locations
# so the container can run as any uid:
ENV MIX_HOME=/opt/mix
ENV HEX_HOME=/opt/hex

COPY mix.exs mix.lock /deps/

RUN cd /deps && \
    mix local.hex --force && \
    mix deps.get && \
    chmod -R a+rwX /opt/mix /opt/hex
