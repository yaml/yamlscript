use strict;
use warnings;
package YAML::PP::Type::MergeKey;

our $VERSION = '0.034'; # VERSION

sub new {
    my ($class) = @_;
    return bless {}, $class;
}

1;


__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Type::MergeKey - A special node type for merge keys

=head1 DESCRIPTION

See L<YAML::PP::Schema::Merge>

=head1 METHODS

=over

=item new

Constructor

=back

=cut

