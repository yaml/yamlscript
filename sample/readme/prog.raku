use YAMLScript;
my $program = slurp 'file.ys';
my YAMLScript $ys .= new;
say $ys.load($program);
