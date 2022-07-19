use strict;
use warnings;
package YAML::PP::Schema::Tie::IxHash;

our $VERSION = '0.034'; # VERSION

use base 'YAML::PP::Schema';

use Scalar::Util qw/ blessed reftype /;
my $ixhash = eval { require Tie::IxHash };

sub register {
    my ($self, %args) = @_;
    my $schema = $args{schema};
    unless ($ixhash) {
        die "You need to install Tie::IxHash in order to use this module";
    }

    $schema->add_representer(
        tied_equals => 'Tie::IxHash',
        code => sub {
            my ($rep, $node) = @_;
            $node->{items} = [ %{ $node->{data} } ];
            return 1;
        },
    );
    return;
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Schema::Tie::IxHash - (Deprecated) Schema for serializing ordered hashes

=head1 SYNOPSIS

    use YAML::PP;
    use Tie::IxHash;
    my $yp = YAML::PP->new( schema => [qw/ + Tie::IxHash /] );

    tie(my %ordered, 'Tie::IxHash');
    %ordered = (
        U => 2,
        B => 52,
    );

    my $yaml = $yp->dump_string(\%ordered);


    # Output:
    ---
    U: 2
    B: 52

=head1 DESCRIPTION

This is deprecated. See the new option C<preserve> in L<YAML::PP>.

This schema allows you to dump ordered hashes which are tied to
L<Tie::IxHash>.

This code is pretty new and experimental.

It is not yet implemented for loading yet, so for now you have to tie
the hashes yourself.

Examples:

=cut

### BEGIN EXAMPLE

=pod

=over 4

=item order

        # Code
        tie(my %order, 'Tie::IxHash');
        %order = (
            U => 2,
            B => 52,
            c => 64,
            19 => 84,
            Disco => 2000,
            Year => 2525,
            days_on_earth => 20_000,
        );
        \%order;


        # YAML
        ---
        U: 2
        B: 52
        c: 64
        19: 84
        Disco: 2000
        Year: 2525
        days_on_earth: 20000


=item order_blessed

        # Code
        tie(my %order, 'Tie::IxHash');
        %order = (
            U => 2,
            B => 52,
            c => 64,
            19 => 84,
            Disco => 2000,
            Year => 2525,
            days_on_earth => 20_000,
        );
        bless \%order, 'Order';


        # YAML
        --- !perl/hash:Order
        U: 2
        B: 52
        c: 64
        19: 84
        Disco: 2000
        Year: 2525
        days_on_earth: 20000




=back

=cut

### END EXAMPLE

=head1 METHODS

=over

=item register

Called by YAML::PP::Schema

=back

=cut
