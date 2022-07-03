package YAMLScript::Function;
use Mo qw'build xxx';

my $func = 'YAMLScript::Function';
my $call = 'YAMLScript::Call';

has ____ => ();
has args => [];
has body => [];
has vars => {};

sub var {
    return $_[0]->{vars}{$_[1]} unless @_ > 2;
    return $_[0]->{vars}{$_[1]} = $_[2];
}

sub call {
    my ($self, @args) = @_;

    my $args = $self->args;
    if (@args != @$args) {
        my $name = $self->____;
        my $list = join ',', @$args;
        my $want = @$args;
        my $got = @args;
        die "YAMLScript function '$name($list) " .
            "requires $want arguments, " .
            "but $got provided\n";
    }

    # Set arg vars:
    for my $name (@$args) {
        $self->var($name, shift(@args));
    }

    # Call each statement in function body:
    for my $stmt (@{$self->body}) {
        $stmt->call($self);
    }
}

sub val {
    my ($self, $value) = @_;
    my $ref = ref($value);
    return $value if $ref eq $func;
    if ($ref eq '') {
        $value =~ s{
            \$(\w+)
        }{
            $self->{vars}{$1} // ZZZ 42 #XXX $value
        }gex;
    }
    else {
        XXX [$ref, "ERROR: Unsupported value"];
    }
    return $value;
}
