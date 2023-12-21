import io
from . import loader

class YAMLScript():
  def load(self, input):
    if isinstance(input, str):
      return self._load_string(input)
    elif isinstance(input, io.IOBase):
      return self._load_string(input.read())

  def _load_string(self, input):
    self.error = None
    resp = loader.Loader().load(input)
    error = self.error = resp.get('error')
    if error:
      raise Exception(error['cause'])
    return resp['data']

def load(input):
  return YAMLScript().load(input)
