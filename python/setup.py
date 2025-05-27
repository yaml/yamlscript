import sys
from pathlib import Path

from setuptools import setup, Extension
from setuptools.command.build_ext import build_ext

version = '0.1.96'

NAME = 'yamlscript'
PACKAGE_DIR = 'lib'
EXTENSIONS = dict(linux='so', darwin='dylib')

root = Path(__file__).parent.resolve()
long_description = \
  (root / '.long_description.md') \
  .read_text(encoding='utf-8')


def get_package_data():
  """

  Include the shared library in the package data if a) this is a supported platform, and b) the shared library exists.

  """
  so = EXTENSIONS.get(sys.platform)

  if so:
    filename = f"lib{NAME}.{so}.{version}"
    lib_exists = (root / PACKAGE_DIR / NAME / filename).exists()
    if lib_exists:
      return {NAME: [filename]}
  return {}

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
  package_dir={'': PACKAGE_DIR},

  python_requires='>=3.6, <4',

  ext_modules=[
    Extension(name=NAME, sources=[])
  ],
  cmdclass={
    'build_ext': LibYAMLScriptExtensionBuilder,
  },

  package_data=get_package_data(),
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
