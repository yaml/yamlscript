use strict;
use warnings;
package YAML::PP::Schema::Binary;

our $VERSION = '0.034'; # VERSION

use MIME::Base64 qw/ decode_base64 encode_base64 /;
use YAML::PP::Common qw/ YAML_ANY_SCALAR_STYLE /;

sub register {
    my ($self, %args) = @_;
    my $schema = $args{schema};

    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:binary',
        match => [ all => sub {
            my ($constructor, $event) = @_;
            my $base64 = $event->{value};
            my $binary = decode_base64($base64);
            return $binary;
        }],
        implicit => 0,
    );

    $schema->add_representer(
        regex => qr{.*},
        code => sub {
            my ($rep, $node) = @_;
            my $binary = $node->{value};
            unless ($binary =~ m/[\x{7F}-\x{10FFFF}]/) {
                # ASCII
                return;
            }
            if (utf8::is_utf8($binary)) {
                # utf8
                return;
            }
            # everything else must be base64 encoded
            my $base64 = encode_base64($binary);
            $node->{style} = YAML_ANY_SCALAR_STYLE;
            $node->{data} = $base64;
            $node->{tag} = "tag:yaml.org,2002:binary";
            return 1;
        },
    );
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Schema::Binary - Schema for loading and binary data

=head1 SYNOPSIS

    use YAML::PP;
    my $yp = YAML::PP->new( schema => [qw/ + Binary /] );
    # or

    my ($binary, $same_binary) = $yp->load_string(<<'EOM');
    --- !!binary "\
      R0lGODlhDAAMAIQAAP//9/X17unp5WZmZgAAAOfn515eXvPz7Y6OjuDg4J+fn5\
      OTk6enp56enmlpaWNjY6Ojo4SEhP/++f/++f/++f/++f/++f/++f/++f/++f/+\
      +f/++f/++f/++f/++f/++SH+Dk1hZGUgd2l0aCBHSU1QACwAAAAADAAMAAAFLC\
      AgjoEwnuNAFOhpEMTRiggcz4BNJHrv/zCFcLiwMWYNG84BwwEeECcgggoBADs="
    --- !!binary |
      R0lGODlhDAAMAIQAAP//9/X17unp5WZmZgAAAOfn515eXvPz7Y6OjuDg4J+fn5
      OTk6enp56enmlpaWNjY6Ojo4SEhP/++f/++f/++f/++f/++f/++f/++f/++f/+
      +f/++f/++f/++f/++f/++SH+Dk1hZGUgd2l0aCBHSU1QACwAAAAADAAMAAAFLC
      AgjoEwnuNAFOhpEMTRiggcz4BNJHrv/zCFcLiwMWYNG84BwwEeECcgggoBADs=
    # The binary value above is a tiny arrow encoded as a gif image.
    EOM

=head1 DESCRIPTION

See <https://yaml.org/type/binary.html>

By prepending a base64 encoded binary string with the C<!!binary> tag, it can
be automatically decoded when loading.

Note that the logic for dumping is probably broken, see
L<https://github.com/perlpunk/YAML-PP-p5/issues/28>.

Suggestions welcome.

=head1 METHODS

=over

=item register

Called by L<YAML::PP::Schema>

=back

=cut
