use strict;
use warnings;
package YAML::PP::Writer::File;

our $VERSION = '0.034'; # VERSION

use Scalar::Util qw/ openhandle /;

use base qw/ YAML::PP::Writer /;

use Carp qw/ croak /;

sub _open_handle {
    my ($self) = @_;
    if (openhandle($self->{output})) {
        $self->{filehandle} = $self->{output};
        return $self->{output};
    }
    open my $fh, '>:encoding(UTF-8)', $self->{output}
        or croak "Could not open '$self->{output}' for writing: $!";
    $self->{filehandle} = $fh;
    return $fh;
}

sub write {
    my ($self, $line) = @_;
    my $fh = $self->{filehandle};
    print $fh $line;
}

sub init {
    my ($self) = @_;
    my $fh = $self->_open_handle;
}

sub finish {
    my ($self) = @_;
    if (openhandle($self->{output})) {
        # Original argument was a file handle, so the caller needs
        # to close it
        return;
    }
    close $self->{filehandle};
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Writer::File - Write YAML output to file or file handle

=head1 SYNOPSIS

    my $writer = YAML::PP::Writer::File->new(output => $file);

=head1 DESCRIPTION

The L<YAML::PP::Emitter> sends its output to the writer.

You can use your own writer. if you want to send the YAML output to
somewhere else. See t/44.writer.t for an example.

=head1 METHODS

=over

=item new

    my $writer = YAML::PP::Writer::File->new(output => $file);
    my $writer = YAML::PP::Writer::File->new(output => $filehandle);

Constructor.

=item write

    $writer->write('- ');

=item init

    $writer->init;

Initialize

=item finish

    $writer->finish;

Gets called when the output ends. If The argument was a filename, the
filehandle will be closed. If the argument was a filehandle, the caller needs to
close it.

=item output, set_output

Getter/setter for the YAML output

=back

=cut
