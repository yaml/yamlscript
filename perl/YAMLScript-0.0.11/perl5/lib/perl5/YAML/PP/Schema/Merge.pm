use strict;
use warnings;
package YAML::PP::Schema::Merge;

our $VERSION = '0.034'; # VERSION

use YAML::PP::Type::MergeKey;

sub register {
    my ($self, %args) = @_;
    my $schema = $args{schema};

    $schema->add_resolver(
        tag => 'tag:yaml.org,2002:merge',
        match => [ equals => '<<' => YAML::PP::Type::MergeKey->new ],
    );
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Schema::Merge - Enabling YAML merge keys for mappings

=head1 SYNOPSIS

    use YAML::PP;
    my $yp = YAML::PP->new( schema => [qw/ + Merge /] );

    my $yaml = <<'EOM';
    ---
    - &CENTER { x: 1, y: 2 }
    - &LEFT { x: 0, y: 2 }
    - &BIG { r: 10 }
    - &SMALL { r: 1 }

    # All the following maps are equal:

    - # Explicit keys
      x: 1
      y: 2
      r: 10
      label: center/big

    - # Merge one map
      << : *CENTER
      r: 10
      label: center/big

    - # Merge multiple maps
      << : [ *CENTER, *BIG ]
      label: center/big

    - # Override
      << : [ *BIG, *LEFT, *SMALL ]
      x: 1
      label: center/big
    EOM
    my $data = $yp->load_string($yaml);
    # $data->[4] == $data->[5] == $data->[6] == $data->[7]

=head1 DESCRIPTION

See L<https://yaml.org/type/merge.html> for the specification.

Quote:

"Specify one or more mappings to be merged with the current one.

The C<< << >> merge key is used to indicate that all the keys of one or more
specified maps should be inserted into the current map. If the value associated
with the key is a single mapping node, each of its key/value pairs is inserted
into the current mapping, unless the key already exists in it. If the value
associated with the merge key is a sequence, then this sequence is expected to
contain mapping nodes and each of these nodes is merged in turn according to its
order in the sequence. Keys in mapping nodes earlier in the sequence override
keys specified in later mapping nodes."

The implementation of this in a generic way is not trivial, because we also
have to handle duplicate keys, and YAML::PP allows you to write your own
handler for processing mappings.

So the inner API of that is not stable at this point.

Note that if you enable this schema, a plain scalar `<<` will be seen as
special anywhere in your document, so if you want a literal `<<`, you have
to put it in quotes.

Note that the performed merge is not a "deep merge". Only top-level keys are
merged.

=head1 METHODS

=over

=item register

Called by YAML::PP::Schema

=back

=cut

