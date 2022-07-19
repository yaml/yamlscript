package YAMLScript::Call;
use Mo qw(default xxx);
use YAMLScript::Util;

has ____ => ();
has code => ();
has args => [];
has lazy => ();

sub call {
    my ($self) = @_;
    my $name = $self->____;
    my $args = $self->args;
    if (not $self->lazy) {
        $args = [ map $self->val($_), @$args ];
    }
    my $func = $self->code;
    if (ref($func) eq 'CODE') {
        $func->(@$args);
    }
    elsif (ref($func) eq 'YAMLScript::Func') {
        $func->call(@$args);
    }
    else {
        XXX $func;
    }
}
