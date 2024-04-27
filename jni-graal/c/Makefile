include ../common.mk

SRC := ../src


#------------------------------------------------------------------------------
default:

clean:
	$(RM) *.so


#------------------------------------------------------------------------------
lib$(SONAME).so: $(LIBNAME).c $(SRC)/$(LIBNAME).h
	gcc -shared -Wall -Werror \
	  -I$(JAVA_HOME)/include \
	  -I$(JAVA_HOME)/include/linux \
	  -I$(SRC) \
	  -o $@ \
	  -fPIC \
	  $<

$(SRC)/$(LIBNAME).h:
	$(MAKE) -C $(SRC) $(@:$(SRC)/%=%)
