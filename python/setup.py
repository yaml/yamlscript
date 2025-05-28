import sys
from pathlib import Path

from setuptools import setup, Extension
from setuptools.command.build_ext import build_ext

version = '0.1.96'

NAME = 'yamlscript'
PACKAGE_DIR = 'lib'
EXTENSIONS = dict(linux='so', darwin='dylib')
so = EXTENSIONS.get(sys.platform)

if not so:
  raise RuntimeError(f"Unsupported platform: {sys.platform}. Should be one of {','.join(EXTENSIONS.keys())}.")

root = Path(__file__).parent.resolve()
filename = f"lib{NAME}.{so}.{version}"
path_lib = root / PACKAGE_DIR / NAME / filename
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


if path_lib.exists():
  # If the shared library exists, only then add the relevant extension builder.
  # Otherwise keep the package generic.
  extension_config = dict(
    ext_modules=[
      Extension(name=NAME, sources=[])
    ],
    cmdclass=dict(
      build_ext=LibYAMLScriptExtensionBuilder,
    ),
    package_data={NAME: [filename]},
  )
else:
  extension_config = dict()

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

  entry_points=dict(
    console_scripts=[
      f'ys-py-show-info = {NAME}:show_info',
    ],
  ),

  **extension_config,
)
