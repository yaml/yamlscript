import pathlib
from setuptools import setup

version = '0.2.22'

root = pathlib.Path(__file__).parent.resolve()

long_description = \
  (root / '.long_description.md') \
  .read_text(encoding='utf-8')

cmdclass = {}

try:
  from wheel.bdist_wheel import bdist_wheel as _bdist_wheel

  class bdist_wheel(_bdist_wheel):
    def finalize_options(self):
      _bdist_wheel.finalize_options(self)
      self.root_is_pure = False

    def get_tag(self):
      python, abi, plat = _bdist_wheel.get_tag(self)
      return 'py3', 'none', plat

  cmdclass['bdist_wheel'] = bdist_wheel
except ImportError:
  pass

setup(
  name = 'yamlscript',
  version = version,
  description = 'Program in YAML — Code is Data',
  license = 'MIT',
  url = 'https://github.com/ingydotnet/yamlscript',

  author = 'Ingy döt Net',
  author_email = 'ingy@ingy.net',

  packages = ['yamlscript'],
  package_dir = {'': 'lib'},
  package_data = {
    'yamlscript': [
      'libys/libys.so.*',
      'libys/libys.dylib.*',
      'libys/libys.dll',
    ],
  },
  cmdclass = cmdclass,

  python_requires = '>=3.6, <4',
  install_requires = [
    'pyyaml',
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
