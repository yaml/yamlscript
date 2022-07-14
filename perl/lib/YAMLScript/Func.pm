package YAMLScript::Func;
use Mo qw(default xxx);
extends 'YAMLScript::Expr';

has ____ => ();
has sign => [];
has body => [];

use YAMLScript::NS;

sub call {
    my ($self, @args) = @_;

    my $sign = $self->sign;
    if (@args != @$sign) {
        my $name = $self->____;
        my $list = join ',', @$sign;
        my $want = @$sign;
        my $got = @args;
        ZZZ [
            $self,
            "YAMLScript function '$name($list) " .
            "requires $want arguments, " .
            "but $got provided\n",
        ];
    }

    my $ns = ns;
    # Set arg vars:
    for my $name (@$sign) {
        $ns->{$name} = shift(@args);
    }

    # Call each statement in function body:
    for my $stmt (@{$self->body}) {
        my @r = $stmt->call();
    }
}
