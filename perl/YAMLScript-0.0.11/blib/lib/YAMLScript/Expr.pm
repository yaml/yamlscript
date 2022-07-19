package YAMLScript::Expr;
use Mo qw(default xxx);

has ____ => ();
has args => [];

use YAMLScript::NS;

sub call {
    my ($self) = @_;
    my $ns = ns;
    my $name = $self->____;
    my $args = $self->args;
    my $arity = @$args;
    my $call =
        $ns->{"${name}__$arity"} ||
        $ns->{"${name}___"} or
        die "Can't resolve call '$name' (for arity '$arity')";
    $call->($args)->call();
}
