use YAMLScript;
my $program = slurp 'hearsay.ys';
my YAMLScript $ys .= new;
say $ys.load($program);
