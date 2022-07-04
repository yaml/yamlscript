package YAMLScript::Library;
use Mo qw'xxx';

sub add {
    my ($class, $look) = @_;
    no strict 'refs';
    my $map = \%{"${class}::"};
    for my $name (grep /^_/, keys %$map) {
        my $func = $map->{$name};
        $name =~ s/^_//;
        $look->{$name} = $func;
    }
}

1;
