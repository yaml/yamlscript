package YAMLScript;
use Mo;

our $VERSION = '0.0.2';

__END__

=pod

=encoding utf-8

=head1 NAME

YAMLScript - Programming in YAML

=head1 SYNOPSIS

    #!/usr/bin/env yamlscript
    - $greetee: world
    - +say: Hello, $greetee!

=head1 DESCRIPTION

YAMLScript is a programming language encoded in YAML.

=head1 COPYRIGHT AND LICENSE

Copyright 2022 by Ingy döt Net

This library is free software and may be distributed under the same terms
as perl itself.

=cut
