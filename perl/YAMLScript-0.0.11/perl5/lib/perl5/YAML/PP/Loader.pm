# ABSTRACT: Load YAML into data with Parser and Constructor
use strict;
use warnings;
package YAML::PP::Loader;

our $VERSION = '0.034'; # VERSION

use YAML::PP::Parser;
use YAML::PP::Constructor;
use YAML::PP::Reader;

sub new {
    my ($class, %args) = @_;

    my $cyclic_refs = delete $args{cyclic_refs} || 'allow';
    my $default_yaml_version = delete $args{default_yaml_version} || '1.2';
    my $preserve = delete $args{preserve};
    my $duplicate_keys = delete $args{duplicate_keys};
    my $schemas = delete $args{schemas};
    $schemas ||= {
        '1.2' => YAML::PP->default_schema(
            boolean => 'perl',
        )
    };

    my $constructor = delete $args{constructor} || YAML::PP::Constructor->new(
        schemas => $schemas,
        cyclic_refs => $cyclic_refs,
        default_yaml_version => $default_yaml_version,
        preserve => $preserve,
        duplicate_keys => $duplicate_keys,
    );
    my $parser = delete $args{parser};
    unless ($parser) {
        $parser = YAML::PP::Parser->new(
            default_yaml_version => $default_yaml_version,
        );
    }
    unless ($parser->receiver) {
        $parser->set_receiver($constructor);
    }

    if (keys %args) {
        die "Unexpected arguments: " . join ', ', sort keys %args;
    }
    my $self = bless {
        parser => $parser,
        constructor => $constructor,
    }, $class;
    return $self;
}

sub clone {
    my ($self) = @_;
    my $clone = {
        parser => $self->parser->clone,
        constructor => $self->constructor->clone,
    };
    bless $clone, ref $self;
    $clone->parser->set_receiver($clone->constructor);
    return $clone;
}

sub parser { return $_[0]->{parser} }
sub constructor { return $_[0]->{constructor} }

sub filename {
    my ($self) = @_;
    my $reader = $self->parser->reader;
    if ($reader->isa('YAML::PP::Reader::File')) {
        return $reader->input;
    }
    die "Reader is not a YAML::PP::Reader::File";
}

sub load_string {
    my ($self, $yaml) = @_;
    $self->parser->set_reader(YAML::PP::Reader->new( input => $yaml ));
    $self->load();
}

sub load_file {
    my ($self, $file) = @_;
    $self->parser->set_reader(YAML::PP::Reader::File->new( input => $file ));
    $self->load();
}

sub load {
    my ($self) = @_;
    my $parser = $self->parser;
    my $constructor = $self->constructor;

    $constructor->init;
    $parser->parse();

    my $docs = $constructor->docs;
    return wantarray ? @$docs : $docs->[0];
}


1;
