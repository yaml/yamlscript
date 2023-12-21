import io
from . import loader

class YAMLScript():
  def load(self, input):
    if isinstance(input, str):
      return self._load_string(input)
    elif isinstance(input, io.IOBase):
      return self._load_string(input.read())

  def compile(self, input):
    if isinstance(input, str):
      return self._compile_string(input)
    elif isinstance(input, io.IOBase):
      return self._compile_string(input.read())

  def _load_string(self, input):
    self.error = None
    resp = loader.Loader().load(input)
    error = self.error = resp.get('error')
    if error:
      raise Exception(error['cause'])
    return resp['data']

  def _compile_string(self, input):
    self.error = None
    resp = loader.Loader().compile(input)
    error = self.error = resp.get('error')
    if error:
      raise Exception(error['cause'])
    return resp['code']

def load(input):
  return YAMLScript().load(input)

def compile(input):
  return YAMLScript().compile(input)
