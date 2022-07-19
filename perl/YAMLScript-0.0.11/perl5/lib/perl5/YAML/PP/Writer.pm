# ABSTRACT: Writer class for YAML::PP representing output data
use strict;
use warnings;
package YAML::PP::Writer;

our $VERSION = '0.034'; # VERSION

sub output { return $_[0]->{output} }
sub set_output { $_[0]->{output} = $_[1] }

sub new {
    my ($class, %args) = @_;
    my $output = delete $args{output};
    $output = '' unless defined $output;
    return bless {
        output => $output,
    }, $class;
}

sub write {
    my ($self, $line) = @_;
    $self->{output} .= $line;
}

sub init {
    $_[0]->set_output('');
}

sub finish {
    my ($self) = @_;
    $_[0]->set_output(undef);
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Writer - Write YAML output

=head1 SYNOPSIS

    my $writer = YAML::PP::Writer->new;

=head1 DESCRIPTION

The L<YAML::PP::Emitter> sends its output to the writer.

You can use your own writer. if you want to send the YAML output to
somewhere else. See t/44.writer.t for an example.

=head1 METHODS

=over

=item new

    my $writer = YAML::PP::Writer->new;

Constructor.

=item write

    $writer->write('- ');

=item init

    $writer->init;

Initialize

=item finish

    $writer->finish;

Gets called when the output ends.

=item output, set_output

Getter/setter for the YAML output

=back

=cut
