package YAMLScript::Library;
use Mo qw'xxx';

sub add {
    my ($class, $calls) = @_;
    no strict 'refs';
    my $map = \%{"${class}::"};
    for my $name (grep /^_/, keys %$map) {
        my $func = $map->{$name};
        $name =~ s/^_//;
        $calls->{$name} = $func;
    }
}

1;
