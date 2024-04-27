RAPIDYAML_REPO := https://github.com/biojppm/rapidyaml
RAPIDYAML_TAG := yamlscript

build: rapidyaml
	cmake -S $< -B $<-build \
	    -D BUILD_SHARED_LIBS=ON \
	    -D RYML_WITH_TAB_TOKENS=ON \
	    -D RYML_DEFAULT_CALLBACK_USES_EXCEPTIONS=ON \
	    -D CMAKE_INSTALL_PREFIX=`pwd`/$<-install
	cmake --build $<-build --parallel --target install

rapidyaml:
	git clone --recursive $(RAPIDYAML_REPO) -b $(RAPIDYAML_TAG)

clean:
	$(RM) -r rapidyaml-build
	$(RM) -r rapidyaml-install

realclean: clean
	$(RM) -r rapidyaml
