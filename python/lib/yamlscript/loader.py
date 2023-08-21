import os, sys
import ctypes
import json

# XXX At the moment it is proving difficult to use a LIBRARY_PATH variable
# to find our libyamlscript library, so we are just using the full path.

root_path = os.environ['YAMLSCRIPT_ROOT']
so = 'dylib' if sys.platform == 'darwin' else 'so'
libys_path = root_path + '/libyamlscript/lib/libyamlscript.' + so

if not os.path.isfile(libys_path):
  raise Exception("Shared library file '%s' not found." % libys_path)

# Load libyamlscript shared library:
libys = ctypes.CDLL(libys_path)

isolate = ctypes.c_void_p()
isolatethread = ctypes.c_void_p()
libys.graal_create_isolate(
  None,
  ctypes.byref(isolate),
  ctypes.byref(isolatethread),
)

compile_ys_to_clj = libys.compile_ys_to_clj
compile_ys_to_clj.restype = ctypes.c_char_p

eval_ys_to_json = libys.eval_ys_to_json
eval_ys_to_json.restype = ctypes.c_char_p

# User API class:
class Loader():
  """
  Send YAMLScript string to be evaluated by shared library.
  The library converts YAMLScript to Clojure and runs it using SCI.
  The code produces a data value, which is encoded in JSON and returned.
  Load the returned JSON into a Python value and return that.
  """

  def compile(self, ys_str):
    ys_input = ys_str.rstrip().replace("\n", "\\n")

    data_json = compile_ys_to_clj(
      isolatethread,
      ctypes.c_char_p(bytes(ys_input, "utf8")),
    ).decode()

    data_value = json.loads(data_json)

    # free_buffer(data_buffer)

    return data_value.get("clojure")

  def load(self, ys_str):
    ys_input = ys_str.rstrip().replace("\n", "\\n")

    data_json = eval_ys_to_json(
      isolatethread,
      ctypes.c_char_p(bytes(ys_input, "utf8")),
    ).decode()

    data_value = json.loads(data_json)

    # free_buffer(data_buffer)

    return data_value
