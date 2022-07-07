package YAMLScript::Expr;
use Mo qw'default xxx';

has ____ => ();
has args => [];

use YAMLScript::NS;

sub call {
    my ($self) = @_;
    my $ns = ns;
    my $name = $self->____;
    my $args = $self->args;
    my $arity = @$args;
    my $factory = $ns->resolve($name, $arity);
    my $call = $factory->($args);
    $call->call();
}
