use strict;
use warnings;
package YAML::PP::Schema::Include;

our $VERSION = '0.034'; # VERSION

use Carp qw/ croak /;
use Scalar::Util qw/ weaken /;
use File::Basename qw/ dirname /;

sub new {
    my ($class, %args) = @_;

    my $paths = delete $args{paths};
    if (defined $paths) {
        unless (ref $paths eq 'ARRAY') {
            $paths = [$paths];
        }
    }
    else {
        $paths = [];
    }
    my $allow_absolute = $args{allow_absolute} || 0;
    my $loader = $args{loader} || \&default_loader;

    my $self = bless {
        paths => $paths,
        allow_absolute => $allow_absolute,
        last_includes => [],
        cached => {},
        loader => $loader,
    }, $class;
    return $self;
}

sub init {
    my ($self) = @_;
    $self->{last_includes} = [];
    $self->{cached} = [];
}

sub paths { $_[0]->{paths} }
sub allow_absolute { $_[0]->{allow_absolute} }
sub yp {
    my ($self, $yp) = @_;
    if (@_ == 2) {
        $self->{yp} = $yp;
        weaken $self->{yp};
        return $yp;
    }
    return $self->{yp};
}

sub register {
    my ($self, %args) = @_;
    my $schema = $args{schema};

    $schema->add_resolver(
        tag => '!include',
        match => [ all => sub { $self->include(@_) } ],
        implicit => 0,
    );
}

sub include {
    my ($self, $constructor, $event) = @_;
    my $yp = $self->yp;
    my $search_paths = $self->paths;
    my $allow_absolute = $self->allow_absolute;

    my $relative = not @$search_paths;
    if ($relative) {
        my $last_includes = $self->{last_includes};
        if (@$last_includes) {
            $search_paths = [ $last_includes->[-1] ];
        }
        else {
            # we are in the top-level file and need to look into
            # the original YAML::PP instance
            my $filename = $yp->loader->filename;
            $search_paths = [dirname $filename];
        }
    }
    my $filename = $event->{value};

    my $fullpath;
    if (File::Spec->file_name_is_absolute($filename)) {
        unless ($allow_absolute) {
            croak "Absolute filenames not allowed";
        }
        $fullpath = $filename;
    }
    else {
        my @paths = File::Spec->splitdir($filename);
        unless ($allow_absolute) {
            # if absolute paths are not allowed, we also may not use upwards ..
            @paths = File::Spec->no_upwards(@paths);
        }
        for my $candidate (@$search_paths) {
            my $test = File::Spec->catfile( $candidate, @paths );
            if (-e $test) {
                $fullpath = $test;
                last;
            }
        }
        croak "File '$filename' not found" unless defined $fullpath;
    }

    if ($self->{cached}->{ $fullpath }++) {
        croak "Circular include '$fullpath'";
    }
    if ($relative) {
        push @{ $self->{last_includes} }, dirname $fullpath;
    }

    # We need a new object because we are still in the parsing and
    # constructing process
    my $clone = $yp->clone;
    my ($data) = $self->loader->($clone, $fullpath);

    if ($relative) {
        pop @{ $self->{last_includes} };
    }
    unless (--$self->{cached}->{ $fullpath }) {
        delete $self->{cached}->{ $fullpath };
    }
    return $data;
}

sub loader {
    my ($self, $code) = @_;
    if (@_ == 2) {
        $self->{loader} = $code;
        return $code;
    }
    return $self->{loader};
}
sub default_loader {
    my ($yp, $filename) = @_;
    $yp->load_file($filename);
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Schema::Include - Include YAML files

=head1 SYNOPSIS

    # /path/to/file.yaml
    # ---
    # included: !include include/file2.yaml

    # /path/to/include/file2.yaml
    # ---
    # a: b

    my $include = YAML::PP::Schema::Include->new;

    my $yp = YAML::PP->new( schema => ['+', $include] );
    # we need the original YAML::PP object for getting the current filename
    # and for loading another file
    $include->yp($yp);

    my ($data) = $yp->load_file("/path/to/file.yaml");

    # The result will be:
    $data = {
        included => { a => 'b' }
    };

Allow absolute filenames and upwards C<'..'>:

    my $include = YAML::PP::Schema::Include->new(
        allow_absolute => 1, # default: 0
    );

Specify paths to search for includes:

    my @include_paths = ("/path/to/include/yaml/1", "/path/to/include/yaml/2");
    my $include = YAML::PP::Schema::Include->new(
        paths => \@include_paths,
    );
    my $yp = YAML::PP->new( schema => ['+', $include] );
    $include->yp($yp);

    # /path/to/include/yaml/1/file1.yaml
    # ---
    # a: b

    my $yaml = <<'EOM';
    - included: !include file1.yaml
    EOM
    my ($data) = $yp->load_string($yaml);


=head1 DESCRIPTION

This plugin allows you to split a large YAML file into smaller ones.
You can then include these files with the C<!include> tag.

It will search for the specified filename relative to the currently processed
filename.

You can also specify the paths where to search for files to include. It iterates
through the paths and returns the first filename that exists.

By default, only relative paths are allowed. Any C<../> in the path will be
removed. You can change that behaviour by setting the option C<allow_absolute>
to true.

If the included file contains more than one document, only the first one
will be included.

I will probably add a possibility to return all documents as an arrayref.

The included YAML file will be loaded by creating a new L<YAML::PP> object
with the schema from the existing object. This way you can recursively include
files.

You can even reuse the same include via an alias:

    ---
    invoice:
        shipping address: &address !include address.yaml
        billing address: *address

Circular includes will be detected, and will be fatal.

It's possible to specify what to do with the included file:

    my $include = YAML::PP::Schema::Include->new(
        loader => sub {
            my ($yp, $filename);
            if ($filename =~ m/\.txt$/) {
                # open file and just return text
            }
            else {
                # default behaviour
                return $yp->load_file($filename);
            }
        },
    );

For example, RAML defines an C<!include> tag which depends on the file
content. If it contains a special RAML directive, it will be loaded as
YAML, otherwise the content of the file will be included as a string.

So with this plugin you are able to read RAML specifications.


=cut
