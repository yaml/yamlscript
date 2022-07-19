use strict;
use warnings;
package YAML::PP::Schema::JSON;

our $VERSION = '0.034'; # VERSION

use base 'Exporter';
our @EXPORT_OK = qw/
    represent_int represent_float represent_literal represent_bool
    represent_undef
/;

use B;
use Carp qw/ croak /;

use YAML::PP::Common qw/ YAML_PLAIN_SCALAR_STYLE YAML_SINGLE_QUOTED_SCALAR_STYLE /;

my $RE_INT = qr{^(-?(?:0|[1-9][0-9]*))$};
my $RE_FLOAT = qr{^(-?(?:0|[1-9][0-9]*)(?:\.[0-9]*)?(?:[eE][+-]?[0-9]+)?)$};

sub _to_int { 0 + $_[2]->[0] }

# DaTa++ && shmem++
sub _to_float { unpack F => pack F => $_[2]->[0] }

sub register {
    my ($self, %args) = @_;
    my $schema = $args{schema};
    my $options = $args{options};
    my $empty_null = 0;
    for my $opt (@$options) {
        if ($opt eq 'empty=str') {
        }
        elsif ($opt eq 'empty=null') {
            $empty_null = 1;
        }
        else {
            croak "Invalid option for JSON Schema: '$opt'";
        }
    }

    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:null',
        match => [ equals => null => undef ],
    );
    if ($empty_null) {
        $schema->add_resolver(
            tag => 'tag:yaml.org,2002:null',
            match => [ equals => '' => undef ],
            implicit => 1,
        );
    }
    else {
        $schema->add_resolver(
            tag => 'tag:yaml.org,2002:str',
            match => [ equals => '' => '' ],
            implicit => 1,
        );
    }
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:bool',
        match => [ equals => true => $schema->true ],
    );
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:bool',
        match => [ equals => false => $schema->false ],
    );
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:int',
        match => [ regex => $RE_INT => \&_to_int ],
    );
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:float',
        match => [ regex => $RE_FLOAT => \&_to_float ],
    );
    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:str',
        match => [ all => sub { $_[1]->{value} } ],
    );

    $schema->add_representer(
        undefined => \&represent_undef,
    );

    my $int_flags = B::SVp_IOK;
    my $float_flags = B::SVp_NOK;
    $schema->add_representer(
        flags => $int_flags,
        code => \&represent_int,
    );
    my %special = ( (0+'nan').'' => '.nan', (0+'inf').'' => '.inf', (0-'inf').'' => '-.inf' );
    $schema->add_representer(
        flags => $float_flags,
        code => \&represent_float,
    );
    $schema->add_representer(
        equals => $_,
        code => \&represent_literal,
    ) for ("", qw/ true false null /);
    $schema->add_representer(
        regex => qr{$RE_INT|$RE_FLOAT},
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

sub represent_undef {
    my ($rep, $node) = @_;
    $node->{style} = YAML_PLAIN_SCALAR_STYLE;
    $node->{data} = 'null';
    return 1;
}

sub represent_literal {
    my ($rep, $node) = @_;
    $node->{style} ||= YAML_SINGLE_QUOTED_SCALAR_STYLE;
    $node->{data} = "$node->{value}";
    return 1;
}


sub represent_int {
    my ($rep, $node) = @_;
    if (int($node->{value}) ne $node->{value}) {
        return 0;
    }
    $node->{style} = YAML_PLAIN_SCALAR_STYLE;
    $node->{data} = "$node->{value}";
    return 1;
}

my %special = (
    (0+'nan').'' => '.nan',
    (0+'inf').'' => '.inf',
    (0-'inf').'' => '-.inf'
);
sub represent_float {
    my ($rep, $node) = @_;
    if (exists $special{ $node->{value} }) {
        $node->{style} = YAML_PLAIN_SCALAR_STYLE;
        $node->{data} = $special{ $node->{value} };
        return 1;
    }
    if (0.0 + $node->{value} ne $node->{value}) {
        return 0;
    }
    if (int($node->{value}) eq $node->{value} and not $node->{value} =~ m/\./) {
        $node->{value} .= '.0';
    }
    $node->{style} = YAML_PLAIN_SCALAR_STYLE;
    $node->{data} = "$node->{value}";
    return 1;
}

sub represent_bool {
    my ($rep, $node) = @_;
    my $string = $node->{value} ? 'true' : 'false';
    $node->{style} = YAML_PLAIN_SCALAR_STYLE;
    @{ $node->{items} } = $string;
    $node->{data} = $string;
    return 1;
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Schema::JSON - YAML 1.2 JSON Schema

=head1 SYNOPSIS

    my $yp = YAML::PP->new( schema => ['JSON'] );
    my $yp = YAML::PP->new( schema => [qw/ JSON empty=str /] );
    my $yp = YAML::PP->new( schema => [qw/ JSON empty=null /] );

=head1 DESCRIPTION

With this schema, the resolution of plain values will work like in JSON.
Everything that matches a special value will be loaded as such, other plain
scalars will be loaded as strings.

Note that this is different from the official YAML 1.2 JSON Schema, where all
strings have to be quoted.

Here you can see all Schemas and examples implemented by YAML::PP:
L<https://perlpunk.github.io/YAML-PP-p5/schemas.html>

Official Schema: L<https://yaml.org/spec/1.2/spec.html#id2803231>

=head1 CONFIGURATION

The official YAML 1.2 JSON Schema wants all strings to be quoted.
YAML::PP currently does not require that (it might do this optionally in
the future).

That means, there are no empty nodes allowed in the official schema. Example:

    ---
    key:

The default behaviour of YAML::PP::Schema::JSON is to return an empty string,
so it would be equivalent to:

    ---
    key: ''

You can configure it to resolve this as C<undef>:

    my $yp = YAML::PP->new( schema => [qw/ JSON empty=null /] );

This way it is equivalent to:

    ---
    key: null

The default is:

    my $yp = YAML::PP->new( schema => [qw/ JSON empty=str /] );

=head1 METHODS

=over

=item register

Called by YAML::PP::Schema

=item represent_bool, represent_float, represent_int, represent_literal, represent_undef

Functions to represent the several node types.

    represent_bool($representer, $node);

=back

=cut
