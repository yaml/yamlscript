#!/usr/bin/env ys-0

use taptest: :all

test::
- name: Colon-prefixed flow key with a value
  cmnd: ys -l
  stdi: '{:foo: bar}'
  want: '{"foo":"bar"}'

- name: Colon-prefixed flow key with an omitted value
  cmnd: ys -l
  stdi: '{:foo}'
  want: '{"foo":null}'

- name: Explicit colon-prefixed flow key
  cmnd: ys -l
  stdi: '{? :foo: bar}'
  want: '{"foo":"bar"}'

- name: Flow mapping value indicator on the next line
  cmnd: ys -l
  stdi: |
    {foo
    : 42, bar
    : 43, baz
    , qux:}
  want: '{"foo":42,"bar":43,"baz":null,"qux":null}'

- name: Anchor names with interior colons
  cmnd: ys -l
  stdi: |
    a: &foo:bar baz
    b: *foo:bar
  want: '{"a":"baz","b":"baz"}'

- name: Escaped slash in a YAML string
  cmnd: ys -l
  stdi: '"https:\/\/yaml.org"'
  want: '"https://yaml.org"'

- name: Tab separation after a sequence indicator
  cmnd: ys -l
  stdi: "-\tfoo\n"
  want: '["foo"]'

- name: Blank tab line after a plain scalar
  cmnd: ys -l
  stdi: "a: foo\n\t\nb: bar\n"
  want: '{"a":"foo","b":"bar"}'

- name: Literal scalar preserves a final space-only content line
  cmnd: ys -l
  stdi: "foo: |\n  x\n   "
  want: '{"foo":"x\n \n"}'

- name: Literal scalar preserves a final tab-only content line
  cmnd: ys -l
  stdi: "foo: |\n  x\n  \t"
  want: '{"foo":"x\n\t\n"}'

done:
