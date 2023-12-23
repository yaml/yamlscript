use v5.16.0;
use YAMLScript::FFI;
use Slurp;
my $program = slurp 'hearsay.ys';
say YAMLScript::FFI->new->load($program);
