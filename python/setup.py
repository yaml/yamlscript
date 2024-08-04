version = '0.1.69'

from setuptools import setup
import pathlib

root = pathlib.Path(__file__).parent.resolve()

long_description = \
  (root / '.long_description.md') \
  .read_text(encoding='utf-8')

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

  python_requires = '>=3.6, <4',
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
