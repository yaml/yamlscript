use strict;
use warnings;
package YAML::PP::Schema::Core;

our $VERSION = '0.034'; # VERSION

use YAML::PP::Schema::JSON qw/
    represent_int represent_float represent_literal represent_bool
    represent_undef
/;

use B;

use YAML::PP::Common qw/ YAML_PLAIN_SCALAR_STYLE /;

my $RE_INT_CORE = qr{^([+-]?(?:[0-9]+))$};
my $RE_FLOAT_CORE = qr{^([+-]?(?:\.[0-9]+|[0-9]+(?:\.[0-9]*)?)(?:[eE][+-]?[0-9]+)?)$};
my $RE_INT_OCTAL = qr{^0o([0-7]+)$};
my $RE_INT_HEX = qr{^0x([0-9a-fA-F]+)$};

sub _from_oct { oct $_[2]->[0] }
sub _from_hex { hex $_[2]->[0] }

sub register {
    my ($self, %args) = @_;
    my $schema = $args{schema};

    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:null',
        match => [ equals => $_ => undef ],
    ) for (qw/ null NULL Null ~ /, '');
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:bool',
        match => [ equals => $_ => $schema->true ],
    ) for (qw/ true TRUE True /);
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:bool',
        match => [ equals => $_ => $schema->false ],
    ) for (qw/ false FALSE False /);
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:int',
        match => [ regex => $RE_INT_CORE => \&YAML::PP::Schema::JSON::_to_int ],
    );
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:int',
        match => [ regex => $RE_INT_OCTAL => \&_from_oct ],
    );
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:int',
        match => [ regex => $RE_INT_HEX => \&_from_hex ],
    );
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:float',
        match => [ regex => $RE_FLOAT_CORE => \&YAML::PP::Schema::JSON::_to_float ],
    );
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:float',
        match => [ equals => $_ => 0 + "inf" ],
    ) for (qw/ .inf .Inf .INF +.inf +.Inf +.INF /);
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:float',
        match => [ equals => $_ => 0 - "inf" ],
    ) for (qw/ -.inf -.Inf -.INF /);
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:float',
        match => [ equals => $_ => 0 + "nan" ],
    ) for (qw/ .nan .NaN .NAN /);
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:str',
        match => [ all => sub { $_[1]->{value} } ],
    );

    my $int_flags = B::SVp_IOK;
    my $float_flags = B::SVp_NOK;
    $schema->add_representer(
        flags => $int_flags,
        code => \&represent_int,
    );
    $schema->add_representer(
        flags => $float_flags,
        code => \&represent_float,
    );
    $schema->add_representer(
        undefined => \&represent_undef,
    );
    $schema->add_representer(
        equals => $_,
        code => \&represent_literal,
    ) for ("", qw/
        true TRUE True false FALSE False null NULL Null ~
        .inf .Inf .INF +.inf +.Inf +.INF -.inf -.Inf -.INF .nan .NaN .NAN
    /);
    $schema->add_representer(
        regex => qr{$RE_INT_CORE|$RE_FLOAT_CORE|$RE_INT_OCTAL|$RE_INT_HEX},
        code => \&represent_literal,
    );

    if ($schema->bool_class) {
        for my $class (@{ $schema->bool_class }) {
            $schema->add_representer(
                class_equals => $class,
                code => \&represent_bool,
            );
        }
    }

    return;
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Schema::Core - YAML 1.2 Core Schema

=head1 SYNOPSIS

    my $yp = YAML::PP->new( schema => ['Core'] );

=head1 DESCRIPTION

This schema is the official recommended Core Schema for YAML 1.2.
It loads additional values to the JSON schema as special types, for
example C<TRUE> and C<True> additional to C<true>.

Official Schema:
L<https://yaml.org/spec/1.2/spec.html#id2804923>

Here you can see all Schemas and examples implemented by YAML::PP:
L<https://perlpunk.github.io/YAML-PP-p5/schemas.html>

=head1 METHODS

=over

=item register

Called by YAML::PP::Schema

=back

=cut
