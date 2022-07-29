package YAMLScript::Expr;
use Mo qw(default xxx);

has ____ => ();
has args => [];

use YAMLScript::NS;

sub call {
    my ($self) = @_;
    my $ns = NS;
    my $name = $self->____;
    my $sub = $name;
    $sub =~ s/-/_/g;
    my $args = $self->args;
    my $arity = @$args;
    my $call =
        $ns->{"${sub}__$arity"} ||
        $ns->{"${sub}___"} or
        die "Can't resolve call '$name' with $arity arguments.";
    if (ref($call) eq 'YAMLScript::Func') {
        $call->call(@$args);
    }
    else {
        $call->($args)->call();
    }
}
