import pathlib

from setuptools import setup, Extension
from setuptools.command.build_ext import build_ext

from lib.yamlscript import NAME, yamlscript_version as version, get_libyamlscript_name

libyamlscript_name = get_libyamlscript_name()
root = pathlib.Path(__file__).parent.resolve()

long_description = \
  (root / '.long_description.md') \
  .read_text(encoding='utf-8')


class LibYAMLScriptExtensionBuilder(build_ext):
  """

  The shared library is pre-built, but we need to provide setuptools
   with a dummy extension builder, so that it knows that the wheels
   aren't just pure-Python and tags them with the correct
   platform/architecture-specific naming and metadata.

  """

  def build_extensions(self):
    """

    Build nothing.

    """
    pass

setup(
  name=NAME,
  version = version,
  description = 'Program in YAML — Code is Data',
  license = 'MIT',
  url = 'https://github.com/ingydotnet/yamlscript',

  author = 'Ingy döt Net',
  author_email = 'ingy@ingy.net',

  packages=[NAME],
  package_dir = {'': 'lib'},

  ext_modules=[
    Extension(name=NAME, sources=[])
  ],
  cmdclass={
    'build_ext': LibYAMLScriptExtensionBuilder,
  },

  package_data={
    NAME: [libyamlscript_name],
  },
  install_requires = [
    'pyyaml',
  ],
  setup_requires = [
    'wheel',
  ],

  keywords = ['yaml', 'language'],
  classifiers = [
    'Development Status :: 3 - Alpha',
    'Intended Audience :: Developers',
    'License :: OSI Approved :: MIT License',
    'Programming Language :: Python :: 3',
    'Programming Language :: Python :: 3.6',
    'Programming Language :: Python :: 3.7',
    'Programming Language :: Python :: 3.8',
    'Programming Language :: Python :: 3.9',
    'Programming Language :: Python :: 3 :: Only',
  ],

  long_description = long_description,
  long_description_content_type = 'text/markdown',
)
