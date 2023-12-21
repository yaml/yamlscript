import io
from . import loader

class YAMLScript():
  def compile(self, input):
    if isinstance(input, str):
      return self._compile_string(input)
    elif isinstance(input, io.IOBase):
      return self._compile_string(input.read())

  def load(self, input):
    if isinstance(input, str):
      return self._load_string(input)
    elif isinstance(input, io.IOBase):
      return self._load_string(input.read())

  def _compile_string(self, input):
    return loader.Loader().compile(input)

  def _load_string(self, input):
    return loader.Loader().load(input)

def compile(input):
  return YAMLScript().compile(input)

def load(input):
  return YAMLScript().load(input)

def run(input):
  YAMLScript().load(input)
  return None
