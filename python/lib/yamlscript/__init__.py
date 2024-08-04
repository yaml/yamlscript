# Copyright 2023-2024 Ingy dot Net
# This code is licensed under MIT license (See License for details)

"""
Python binding/API for the libyamlscript shared library.

This module can be considered the reference implementation for YAMLScript
FFI bindings to libyamlscript.

The current user facing API consists of a single class, `YAMLScript`, which
has a single method: `.load(string)`.
The load() method takes a YAMLScript string as input and returns the Python
object that the YAMLScript code evaluates to.
"""

# This value is automatically updated by 'make bump'.
# The version number is used to find the correct shared library file.
# We currently only support binding to an exact version of libyamlscript.
yamlscript_version = '0.1.69'

import os, sys
import ctypes
import json

# Require Python 3.6 or greater:
assert sys.version_info >= (3, 6), \
  "Python 3.6 or greater required for 'yamlscript'."

# Find the libyamlscript shared library file path:
def find_libyamlscript_path():
  # We currently only support platforms that GraalVM supports.
  # And Windows is not yet implemented...
  # Confirm platform and determine file extension:
  if sys.platform == 'linux':
    so = 'so'
  elif sys.platform == 'darwin':
    so = 'dylib'
  else:
    raise Exception(
      "Unsupported platform '%s' for yamlscript." % sys.platform)

  # We currently bind to an exact version of libyamlscript.
  # eg 'libyamlscript.so.0.1.69'
  libyamlscript_name = \
    "libyamlscript.%s.%s" % (so, yamlscript_version)

  # Use LD_LIBRARY_PATH to find libyamlscript shared library, or default to
  # '/usr/local/lib' (where it is installed by default):
  ld_library_path = os.environ.get('LD_LIBRARY_PATH')
  ld_library_paths = ld_library_path.split(':') if ld_library_path else []
  ld_library_paths.append('/usr/local/lib')
  ld_library_paths.append(os.environ.get('HOME') + '/.local/lib')

  libyamlscript_path = None
  for path in ld_library_paths:
    path = path + '/' + libyamlscript_name
    if os.path.isfile(path):
      libyamlscript_path = path
      break

  if not libyamlscript_path:
    raise Exception(
      """\
Shared library file '%s' not found
Try: curl https://yamlscript.org/install | VERSION=%s LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript
""" % (libyamlscript_name, yamlscript_version))

  return libyamlscript_path

# Load libyamlscript shared library:
libyamlscript = ctypes.CDLL(find_libyamlscript_path())

# Create binding to 'load_ys_to_json' function:
load_ys_to_json = libyamlscript.load_ys_to_json
load_ys_to_json.restype = ctypes.c_char_p


# The YAMLScript class is the main user facing API for this module.
class YAMLScript():
  """
  Interface with the libyamlscript shared library.

  Usage:
    import yamlscript
    ys = yamlscript.YAMLScript()
    data = ys.load(open('file.ys').read())
  """

  # YAMLScript instance constructor:
  def __init__(self, config={}):
    # config not used yet
    # self.config = config

    # Create a new GraalVM isolatethread for life of the YAMLScript instance:
    self.isolatethread = ctypes.c_void_p()

    # Create a new GraalVM isolate:
    rc = libyamlscript.graal_create_isolate(
      None,
      None,
      ctypes.byref(self.isolatethread),
    )

    if rc != 0:
      raise Exception("Failed to create isolate")

  # Compile and eval a YAMLScript string and return the result:
  def load(self, input):
    # Reset any previous error:
    self.error = None

    # Call 'load_ys_to_json' function in libyamlscript shared library:
    data_json = load_ys_to_json(
      self.isolatethread,
      ctypes.c_char_p(bytes(input, "utf8")),
    ).decode()

    # Decode the JSON response:
    resp = json.loads(data_json)

    # Check for libyamlscript error in JSON response:
    self.error = resp.get('error')
    if self.error:
      raise Exception(self.error['cause'])

    # Get the response object from evaluating the YAMLScript string:
    if not 'data' in resp:
      raise Exception("Unexpected response from 'libyamlscript'")
    data = resp.get('data')

    # Return the response object:
    return data

  # YAMLScript instance destructor:
  def __del__(self):
    # Tear down the isolate thread to free resources:
    rc = libyamlscript.graal_tear_down_isolate(self.isolatethread)
    if rc != 0:
      raise Exception("Failed to tear down isolate")
