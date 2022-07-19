use strict;
use warnings;
package YAML::PP::Perl;

our $VERSION = '0.034'; # VERSION

use base 'Exporter';
use base 'YAML::PP';
our @EXPORT_OK = qw/ Load Dump LoadFile DumpFile /;

use YAML::PP;
use YAML::PP::Schema::Perl;

sub new {
    my ($class, %args) = @_;
    $args{schema} ||= [qw/ Core Perl /];
    $class->SUPER::new(%args);
}

sub Load {
    my ($yaml) = @_;
    __PACKAGE__->new->load_string($yaml);
}

sub LoadFile {
    my ($file) = @_;
    __PACKAGE__->new->load_file($file);
}

sub Dump {
    my (@data) = @_;
    __PACKAGE__->new->dump_string(@data);
}

sub DumpFile {
    my ($file, @data) = @_;
    __PACKAGE__->new->dump_file($file, @data);
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Perl - Convenience module for loading and dumping Perl objects

=head1 SYNOPSIS

    use YAML::PP::Perl;
    my @docs = YAML::PP::Perl->new->load_string($yaml);
    my @docs = YAML::PP::Perl::Load($yaml);

    # same as
    use YAML::PP;
    my $yp = YAML::PP->new( schema => [qw/ Core Perl /] );
    my @docs = $yp->load_string($yaml);

=head1 DESCRIPTION

This is just for convenience. It will create a YAML::PP object using the
default schema (C<Core>) and the L<YAML::PP::Schema::Perl> schema.

See L<YAML::PP::Schema::Perl> for documentation.

=head1 METHODS

=over

=item Load, Dump, LoadFile, DumpFile

These work like the functions in L<YAML::PP>, just adding the C<Perl> schema.

=item new

Constructor, works like in L<YAML::PP>, just adds the C<Perl> schema to the
list of arguments.

=back
