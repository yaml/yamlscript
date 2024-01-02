use v5.16.0;
use YAMLScript;
use Slurp;
my $program = slurp 'hearsay.ys';
say YAMLScript->new->load($program);
