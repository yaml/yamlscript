
DOCKER-FILES := \
  $(MAKES)/share/from.dockerfile \
	$(COMMON)/ubuntu.dockerfile \
  $(MAKES)/share/user.dockerfile \

DOCKER-BUILD-OPTIONS := \
  --build-arg FROM=ubuntu:20.04 \
  --build-arg USER=$(USER) \
  --build-arg UID=$(USER-UID) \
  --build-arg GID=$(USER-GID) \

ifndef YS_BUILD_IN_DOCKER
override PROPER-BUILD-ENV := true
endif
ifndef IS-LINUX
override PROPER-BUILD-ENV := true
endif
ifndef IS-INTEL
override PROPER-BUILD-ENV := true
endif

include $(MAKES)/docker.mk
