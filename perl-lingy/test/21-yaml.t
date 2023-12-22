use YAMLScript::Test;

sub cmd {
    my ($cmd, $want) = @_;
    my ($got) = capture_merged { system $cmd };
    is $got, $want, "Program works: '$cmd'";
}

cmd "$yamlscript test/config1.ys",
"---
- color: blue
  fast: ''
  size: '43'
- color: pink
  cool:
  - '1'
  - '2'
  - '3'
  fast: 1
  size: 42
- color: blue
  fast: ''
  size: 42
- color: blue
  fast: 1
  other: stuff
  size: 42

"
