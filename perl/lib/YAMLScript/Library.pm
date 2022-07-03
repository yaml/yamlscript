package YAMLScript::Library;
use Mo qw'build xxx';

my $func = 'YAMLScript::Function';
my $call = 'YAMLScript::Call';

# Use named subs for better stack traces
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

# for my $name (grep /^_/, keys %YAMLScript::Library::) {
#     WWW $YAMLScript::Library
# }

our %calls = (
    for => \&_for,
    range => \&_range,
    say => \&_say,
    set => \&_set,
);
