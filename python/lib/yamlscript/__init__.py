import io
from . import loader

class YAMLScript():
  def compile(self, input):
    if isinstance(input, str):
      return self.compile_string(input)
    elif isinstance(input, io.IOBase):
      return self.compile_string(input.read())

  def compile_string(self, input):
    return loader.Loader().compile(input)

  def compile_file(self, file_path):
    file = open(file_path, mode='r')
    data = self.compile_string(file.read())
    file.close()
    return data

  def load(self, input):
    if isinstance(input, str):
      return self.load_string(input)
    elif isinstance(input, io.IOBase):
      return self.load_string(input.read())

  def load_string(self, input):
    return loader.Loader().load(input)

  def load_file(self, file_path):
    file = open(file_path, mode='r')
    data = self.load_string(file.read())
    file.close()
    return data

def compile(input):
  return YAMLScript().compile(input)

def load(input):
  return YAMLScript().load(input)
