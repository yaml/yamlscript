FROM python

USER root
WORKDIR /
RUN git clone https://github.com/yaml/yamlscript
WORKDIR /yamlscript
RUN make build

RUN pip install fmtr.tools[debug] setuptools

COPY . .
WORKDIR /yamlscript/python


RUN make dist
WORKDIR /tmp

RUN pip uninstall yamlscript -y
RUN pip install /yamlscript/python/dist/yamlscript-*.whl
RUN bash -c "ys-py-show-info > bdist_wheel.info.txt"

RUN pip install /yamlscript/python/dist/yamlscript-0.1.96.tar.gz
RUN bash -c "export LD_LIBRARY_PATH=/yamlscript/libyamlscript/lib && ys-py-show-info > sdist.info.txt"


CMD sleep infinity