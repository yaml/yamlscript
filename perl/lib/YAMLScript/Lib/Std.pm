package YAMLScript::Lib::Std;
use Mo qw'xxx';
extends 'YAMLScript::Library';

my $func = 'YAMLScript::Function';
my $call = 'YAMLScript::Call';

# Use named subs for better stack traces

sub _add {
    my ($self, $x, $y) = @_;
    $self->val($x) + $self->val($y);
}

sub _for {
    my ($self, $list, $action) = @_;
    $list = $list->call($self)
        if ref($list) eq $call;
    for my $elem (@$list) {
        $self->{vars}{_} = $elem;
        $action->call($self);
    }
}

sub _range {
    my ($self, $min, $max) = @_;
    $min = $self->val($min);
    $max = $self->val($max);
    [ $min .. $max ];
}

sub _say {
    my ($self, $string) = @_;
    print $self->val($string) . "\n";
}

sub _set {
    my ($self, $var, $expr) = @_;
    $self->var($var, $self->val($expr));
}
