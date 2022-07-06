package YS::TestYAMLScript;
use Mo qw'xxx';
extends 'YS';

use Test::More ();

my $count = 0;
my $func = 'YAMLScript::Function';
my $call = 'YAMLScript::Call';

sub _is {
    my ($self, $got, $want, $label) = @_;
    Test::More::is(
        $self->val($got),
        $self->val($want),
        $self->val($label),
    );
    $count++;
}

sub END {
    Test::More::done_testing($count);
}
