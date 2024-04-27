include ../../common/base.mk
include $(COMMON)/java.mk
include ../common.mk

default::

build:: lib$(SONAME).so

lib$(SONAME).so:
	cmake -S . -B build \
	  -DBUILD_SHARED_LIBS=ON \
	  -DCMAKE_MODULE_PATH=$(JAVA_HOME)
	cmake --build build
	mv build/lib$(LIBNAME).so lib$(SONAME).so

clean::
	$(RM) *.so
	$(RM) -r build
