# ABSTRACT: YAML 1.2 Processor
use strict;
use warnings;
package YAML::PP;

our $VERSION = '0.034'; # VERSION

use YAML::PP::Schema;
use YAML::PP::Schema::JSON;
use YAML::PP::Loader;
use YAML::PP::Dumper;
use Scalar::Util qw/ blessed /;
use Carp qw/ croak /;

use base 'Exporter';
our @EXPORT_OK = qw/ Load LoadFile Dump DumpFile /;

my %YAML_VERSIONS = ('1.1' => 1, '1.2' => 1);


sub new {
    my ($class, %args) = @_;

    my $bool = delete $args{boolean};
    $bool = 'perl' unless defined $bool;
    my $schemas = delete $args{schema} || ['+'];
    my $cyclic_refs = delete $args{cyclic_refs} || 'allow';
    my $indent = delete $args{indent};
    my $width = delete $args{width};
    my $writer = delete $args{writer};
    my $header = delete $args{header};
    my $footer = delete $args{footer};
    my $duplicate_keys = delete $args{duplicate_keys};
    my $yaml_version = $class->_arg_yaml_version(delete $args{yaml_version});
    my $default_yaml_version = $yaml_version->[0];
    my $version_directive = delete $args{version_directive};
    my $preserve = delete $args{preserve};
    my $parser = delete $args{parser};
    my $emitter = delete $args{emitter} || {
        indent => $indent,
        width => $width,
        writer => $writer,
    };
    if (keys %args) {
        die "Unexpected arguments: " . join ', ', sort keys %args;
    }

    my %schemas;
    for my $v (@$yaml_version) {
        my $schema;
        if (blessed($schemas) and $schemas->isa('YAML::PP::Schema')) {
            $schema = $schemas;
        }
        else {
            $schema = YAML::PP::Schema->new(
                boolean => $bool,
                yaml_version => $v,
            );
            $schema->load_subschemas(@$schemas);
        }
        $schemas{ $v } = $schema;
    }
    my $default_schema = $schemas{ $default_yaml_version };

    my $loader = YAML::PP::Loader->new(
        schemas => \%schemas,
        cyclic_refs => $cyclic_refs,
        parser => $parser,
        default_yaml_version => $default_yaml_version,
        preserve => $preserve,
        duplicate_keys => $duplicate_keys,
    );
    my $dumper = YAML::PP::Dumper->new(
        schema => $default_schema,
        emitter => $emitter,
        header => $header,
        footer => $footer,
        version_directive => $version_directive,
        preserve => $preserve,
    );

    my $self = bless {
        schema => \%schemas,
        loader => $loader,
        dumper => $dumper,
    }, $class;
    return $self;
}

sub clone {
    my ($self) = @_;
    my $clone = {
        schema => $self->schema,
        loader => $self->loader->clone,
        dumper => $self->dumper->clone,
    };
    return bless $clone, ref $self;
}

sub _arg_yaml_version {
    my ($class, $version) = @_;
    my @versions = ('1.2');
    if (defined $version) {
        @versions = ();
        if (not ref $version) {
            $version = [$version];
        }
        for my $v (@$version) {
            unless ($YAML_VERSIONS{ $v }) {
                croak "YAML Version '$v' not supported";
            }
            push @versions, $v;
        }
    }
    return \@versions;
}


sub loader {
    if (@_ > 1) {
        $_[0]->{loader} = $_[1]
    }
    return $_[0]->{loader};
}

sub dumper {
    if (@_ > 1) {
        $_[0]->{dumper} = $_[1]
    }
    return $_[0]->{dumper};
}

sub schema {
    if (@_ > 1) { $_[0]->{schema}->{'1.2'} = $_[1] }
    return $_[0]->{schema}->{'1.2'};
}

sub default_schema {
    my ($self, %args) = @_;
    my $schema = YAML::PP::Schema->new(
        boolean => $args{boolean},
    );
    $schema->load_subschemas(qw/ Core /);
    return $schema;
}

sub load_string {
    my ($self, $yaml) = @_;
    return $self->loader->load_string($yaml);
}

sub load_file {
    my ($self, $file) = @_;
    return $self->loader->load_file($file);
}

sub dump {
    my ($self, @data) = @_;
    return $self->dumper->dump(@data);
}

sub dump_string {
    my ($self, @data) = @_;
    return $self->dumper->dump_string(@data);
}

sub dump_file {
    my ($self, $file, @data) = @_;
    return $self->dumper->dump_file($file, @data);
}

# legagy interface
sub Load {
    my ($yaml) = @_;
    YAML::PP->new->load_string($yaml);
}

sub LoadFile {
    my ($file) = @_;
    YAML::PP->new->load_file($file);
}

sub Dump {
    my (@data) = @_;
    YAML::PP->new->dump_string(@data);
}

sub DumpFile {
    my ($file, @data) = @_;
    YAML::PP->new->dump_file($file, @data);
}

sub preserved_scalar {
    my ($self, $value, %args) = @_;
    my $scalar = YAML::PP::Preserve::Scalar->new(
        value => $value,
        %args,
    );
    return $scalar;
}

sub preserved_mapping {
    my ($self, $hash, %args) = @_;
    my $data = {};
    tie %$data, 'YAML::PP::Preserve::Hash';
    %$data = %$hash;
    my $t = tied %$data;
    $t->{style} = $args{style};
    $t->{alias} = $args{alias};
    return $data;
}

sub preserved_sequence {
    my ($self, $array, %args) = @_;
    my $data = [];
    tie @$data, 'YAML::PP::Preserve::Array';
    push @$data, @$array;
    my $t = tied @$data;
    $t->{style} = $args{style};
    $t->{alias} = $args{alias};
    return $data;
}

package YAML::PP::Preserve::Hash;
# experimental
use Tie::Hash;
use base qw/ Tie::StdHash /;
use Scalar::Util qw/ reftype blessed /;

sub TIEHASH {
    my ($class, %args) = @_;
    my $self = bless {
        keys => [keys %args],
        data => { %args },
    }, $class;
}

sub STORE {
    my ($self, $key, $val) = @_;
    my $keys = $self->{keys};
    unless (exists $self->{data}->{ $key }) {
        push @$keys, $key;
    }
    if (ref $val and not blessed($val)) {
        if (reftype($val) eq 'HASH' and not tied %$val) {
            tie %$val, 'YAML::PP::Preserve::Hash', %$val;
        }
        elsif (reftype($val) eq 'ARRAY' and not tied @$val) {
            tie @$val, 'YAML::PP::Preserve::Array', @$val;
        }
    }
    $self->{data}->{ $key } = $val;
}

sub FIRSTKEY {
    my ($self) = @_;
    return $self->{keys}->[0];
}

sub NEXTKEY {
    my ($self, $last) = @_;
    my $keys = $self->{keys};
    for my $i (0 .. $#$keys) {
        if ("$keys->[ $i ]" eq "$last") {
            return $keys->[ $i + 1 ];
        }
    }
    return;
}

sub FETCH {
    my ($self, $key) = @_;
    my $val = $self->{data}->{ $key };
}

sub DELETE {
    my ($self, $key) = @_;
    @{ $self->{keys} } = grep { "$_" ne "$key" } @{ $self->{keys} };
    delete $self->{data}->{ $key };
}

sub EXISTS {
    my ($self, $key) = @_;
    return exists $self->{data}->{ $key };
}

sub CLEAR {
    my ($self) = @_;
    $self->{keys} = [];
    $self->{data} = {};
}

sub SCALAR {
    my ($self) = @_;
    return scalar %{ $self->{data} };
}

package YAML::PP::Preserve::Array;
# experimental
use Tie::Array;
use base qw/ Tie::StdArray /;
use Scalar::Util qw/ reftype blessed /;

sub TIEARRAY {
    my ($class, @items) = @_;
    my $self = bless {
        data => [@items],
    }, $class;
    return $self;
}

sub FETCH {
    my ($self, $i) = @_;
    return $self->{data}->[ $i ];
}
sub FETCHSIZE {
    my ($self) = @_;
    return $#{ $self->{data} } + 1;
}

sub _preserve {
    my ($val) = @_;
    if (ref $val and not blessed($val)) {
        if (reftype($val) eq 'HASH' and not tied %$val) {
            tie %$val, 'YAML::PP::Preserve::Hash', %$val;
        }
        elsif (reftype($val) eq 'ARRAY' and not tied @$val) {
            tie @$val, 'YAML::PP::Preserve::Array', @$val;
        }
    }
    return $val;
}

sub STORE {
    my ($self, $i, $val) = @_;
    _preserve($val);
    $self->{data}->[ $i ] = $val;
}
sub PUSH {
    my ($self, @args) = @_;
    push @{ $self->{data} }, map { _preserve $_ } @args;
}
sub STORESIZE {
    my ($self, $i) = @_;
    $#{ $self->{data} } = $i - 1;
}
sub DELETE {
    my ($self, $i) = @_;
    delete $self->{data}->[ $i ];
}
sub EXISTS {
    my ($self, $i) = @_;
    return exists $self->{data}->[ $i ];
}
sub CLEAR {
    my ($self) = @_;
    $self->{data} = [];
}
sub SHIFT {
    my ($self) = @_;
    shift @{ $self->{data} };
}
sub UNSHIFT {
    my ($self, @args) = @_;
    unshift @{ $self->{data} }, map { _preserve $_ } @args;
}
sub SPLICE {
    my ($self, $offset, $length, @args) = @_;
    splice @{ $self->{data} }, $offset, $length, map { _preserve $_ } @args;
}
sub EXTEND {}


package YAML::PP::Preserve::Scalar;

use overload
    fallback => 1,
    '+' => \&value,
    '""' => \&value,
    'bool' => \&value,
    ;
sub new {
    my ($class, %args) = @_;
    my $self = {
        %args,
    };
    bless $self, $class;
}
sub value { $_[0]->{value} }
sub tag { $_[0]->{tag} }
sub style { $_[0]->{style} || 0 }
sub alias { $_[0]->{alias} }

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP - YAML 1.2 processor

=head1 SYNOPSIS

WARNING: Most of the inner API is not stable yet.

Here are a few examples of the basic load and dump methods:

    use YAML::PP;
    my $ypp = YAML::PP->new;

    my $yaml = <<'EOM';
    --- # Document one is a mapping
    name: Tina
    age: 29
    favourite language: Perl

    --- # Document two is a sequence
    - plain string
    - 'in single quotes'
    - "in double quotes we have escapes! like \t and \n"
    - | # a literal block scalar
      line1
      line2
    - > # a folded block scalar
      this is all one
      single line because the
      linebreaks will be folded
    EOM

    my @documents = $ypp->load_string($yaml);
    my @documents = $ypp->load_file($filename);

    my $yaml = $ypp->dump_string($data1, $data2);
    $ypp->dump_file($filename, $data1, $data2);

    # The loader offers JSON::PP::Boolean, boolean.pm or
    # perl 1/'' (currently default) for booleans
    my $ypp = YAML::PP->new(boolean => 'JSON::PP');
    my $ypp = YAML::PP->new(boolean => 'boolean');
    my $ypp = YAML::PP->new(boolean => 'perl');

    # Enable perl data types and objects
    my $ypp = YAML::PP->new(schema => [qw/ + Perl /]);
    my $yaml = $yp->dump_string($data_with_perl_objects);

    # Legacy interface
    use YAML::PP qw/ Load Dump LoadFile DumpFile /;
    my @documents = Load($yaml);
    my @documents = LoadFile($filename);
    my @documents = LoadFile($filehandle);
    my $yaml = = Dump(@documents);
    DumpFile($filename, @documents);
    DumpFile($filenhandle @documents);


Some utility scripts, mostly useful for debugging:

    # Load YAML into a data structure and dump with Data::Dumper
    yamlpp-load < file.yaml

    # Load and Dump
    yamlpp-load-dump < file.yaml

    # Print the events from the parser in yaml-test-suite format
    yamlpp-events < file.yaml

    # Parse and emit events directly without loading
    yamlpp-parse-emit < file.yaml

    # Create ANSI colored YAML. Can also be useful for invalid YAML, showing
    # you the exact location of the error
    yamlpp-highlight < file.yaml


=head1 DESCRIPTION

YAML::PP is a modular YAML processor.

It aims to support C<YAML 1.2> and C<YAML 1.1>. See L<https://yaml.org/>.
Some (rare) syntax elements are not yet supported and documented below.

YAML is a serialization language. The YAML input is called "YAML Stream".
A stream consists of one or more "Documents", separated by a line with a
document start marker C<--->. A document optionally ends with the document
end marker C<...>.

This allows one to process continuous streams additionally to a fixed input
file or string.

The YAML::PP frontend will currently load all documents, and return only
the first if called with scalar context.

The YAML backend is implemented in a modular way that allows one to add
custom handling of YAML tags, perl objects and data types. The inner API
is not yet stable. Suggestions welcome.

You can check out all current parse and load results from the
yaml-test-suite here:
L<https://perlpunk.github.io/YAML-PP-p5/test-suite.html>


=head1 METHODS

=head2 new

    my $ypp = YAML::PP->new;
    # load booleans via boolean.pm
    my $ypp = YAML::PP->new( boolean => 'boolean' );
    # load booleans via JSON::PP::true/false
    my $ypp = YAML::PP->new( boolean => 'JSON::PP' );
    
    # use YAML 1.2 Failsafe Schema
    my $ypp = YAML::PP->new( schema => ['Failsafe'] );
    # use YAML 1.2 JSON Schema
    my $ypp = YAML::PP->new( schema => ['JSON'] );
    # use YAML 1.2 Core Schema
    my $ypp = YAML::PP->new( schema => ['Core'] );
    
    # Die when detecting cyclic references
    my $ypp = YAML::PP->new( cyclic_refs => 'fatal' );
    
    my $ypp = YAML::PP->new(
        boolean => 'JSON::PP',
        schema => ['Core'],
        cyclic_refs => 'fatal',
        indent => 4,
        header => 1,
        footer => 1,
        version_directive => 1,
    );

Options:

=over

=item boolean

Values: C<perl> (currently default), C<JSON::PP>, C<boolean>, C<perl_experimental>

This option is for loading and dumping.

You can also specify more than one class, comma separated.
This is important for dumping.

Examples:

    boolean => 'JSON::PP,boolean'
    Booleans will be loaded as JSON::PP::Booleans, but when dumping, also
    'boolean' objects will be recognized

    boolean => 'JSON::PP,*'
    Booleans will be loaded as JSON::PP::Booleans, but when dumping, all
    currently supported boolean classes will be recognized

    boolean => '*'
    Booleans will be loaded as perl booleans, but when dumping, all
    currently supported boolean classes will be recognized

If you have perl >= 5.36 then you might want to try out the experimental
boolean support, see L<builtin>.

YAML::PP supports that by using the C<perl_experimental> value for the boolean
option. Rules are the same as for the experimental L<builtin> class: It's
not guaranteed to work in the future.

As soon as the builtin boolean support leaves experimental status, I will
update YAML::PP to support this via the default C<perl> value.

    boolean => 'perl_experimental'
    Booleans will be loaded as perl booleans, and they will be recognized
    as such when dumping also

=item schema

Default: C<['Core']>

This option is for loading and dumping.

Array reference. Here you can define what schema to use.
Supported standard Schemas are: C<Failsafe>, C<JSON>, C<Core>, C<YAML1_1>.

To get an overview how the different Schemas behave, see
L<https://perlpunk.github.io/YAML-PP-p5/schemas.html>

Additionally you can add further schemas, for example C<Merge>.

=item cyclic_refs

Default: 'allow' but will be switched to fatal in the future for safety!

This option is for loading only.

Defines what to do when a cyclic reference is detected when loading.

    # fatal  - die
    # warn   - Just warn about them and replace with undef
    # ignore - replace with undef
    # allow  - Default

=item duplicate_keys

Default: 0

Since version 0.027

This option is for loading.

The YAML Spec says duplicate mapping keys should be forbidden.

When set to true, duplicate keys in mappings are allowed (and will overwrite
the previous key).

When set to false, duplicate keys will result in an error when loading.

This is especially useful when you have a longer mapping and don't see
the duplicate key in your editor:

    ---
    a: 1
    b: 2
    # .............
    a: 23 # error

=item indent

Default: 2

This option is for dumping.

Use that many spaces for indenting

=item width

Since version 0.025

Default: 80

This option is for dumping.

Maximum columns when dumping.

This is only respected when dumping flow collections right now.

in the future it will be used also for wrapping long strings.

=item header

Default: 1

This option is for dumping.

Print document header C<--->

=item footer

Default: 0

This option is for dumping.

Print document footer C<...>

=item yaml_version

Since version 0.020

This option is for loading and dumping.

Default: C<1.2>

Note that in this case, a directive C<%YAML 1.1> will basically be ignored
and everything loaded with the C<1.2 Core> Schema.

If you want to support both YAML 1.1 and 1.2, you have to specify that, and the
schema (C<Core> or C<YAML1_1>) will be chosen automatically.

    my $yp = YAML::PP->new(
        yaml_version => ['1.2', '1.1'],
    );

This is the same as

    my $yp = YAML::PP->new(
        schema => ['+'],
        yaml_version => ['1.2', '1.1'],
    );

because the C<+> stands for the default schema per version.

When loading, and there is no C<%YAML> directive, C<1.2> will be considered
as default, and the C<Core> schema will be used.

If there is a C<%YAML 1.1> directive, the C<YAML1_1> schema will be used.

Of course, you can also make C<1.1> the default:

    my $yp = YAML::PP->new(
        yaml_version => ['1.1', '1.2'],
    );


You can also specify C<1.1> only:

    my $yp = YAML::PP->new(
        yaml_version => ['1.1'],
    );

In this case also documents with C<%YAML 1.2> will be loaded with the C<YAML1_1>
schema.

=item version_directive

Since version 0.020

This option is for dumping.

Default: 0

Print Version Directive C<%YAML 1.2> (or C<%YAML 1.1>) on top of each YAML
document. It will use the first version specified in the C<yaml_version> option.

=item preserve

Since version 0.021

Default: false

This option is for loading and dumping.

Preserving scalar styles is still experimental.

    use YAML::PP::Common qw/ :PRESERVE /;

    # Preserve the order of hash keys
    my $yp = YAML::PP->new( preserve => PRESERVE_ORDER );

    # Preserve the quoting style of scalars
    my $yp = YAML::PP->new( preserve => PRESERVE_SCALAR_STYLE );

    # Preserve block/flow style (since 0.024)
    my $yp = YAML::PP->new( preserve => PRESERVE_FLOW_STYLE );

    # Preserve alias names (since 0.027)
    my $yp = YAML::PP->new( preserve => PRESERVE_ALIAS );

    # Combine, e.g. preserve order and scalar style
    my $yp = YAML::PP->new( preserve => PRESERVE_ORDER | PRESERVE_SCALAR_STYLE );

Do NOT rely on the internal implementation of it.

If you load the following input:

    ---
    z: 1
    a: 2
    ---
    - plain
    - 'single'
    - "double"
    - |
      literal
    ---
    block mapping: &alias
      flow sequence: [a, b]
    same mapping: *alias
    flow mapping: {a: b}


with this code:

    my $yp = YAML::PP->new(
        preserve => PRESERVE_ORDER | PRESERVE_SCALAR_STYLE
                    | PRESERVE_FLOW_STYLE | PRESERVE_ALIAS
    );
    my ($hash, $styles, $flow) = $yp->load_file($file);
    $yp->dump_file($hash, $styles, $flow);

Then dumping it will return the same output.
Only folded block scalars '>' cannot preserve the style yet.

Note that YAML allows repeated definition of anchors. They cannot be preserved
with YAML::PP right now. Example:

    ---
    - &seq [a]
    - *seq
    - &seq [b]
    - *seq

Because the data could be shuffled before dumping again, the anchor definition
could be broken. In this case repeated anchor names will be discarded when
loading and dumped with numeric anchors like usual.

Implementation:

When loading, hashes will be tied to an internal class
(C<YAML::PP::Preserve::Hash>) that keeps the key order.

Scalars will be returned as objects of an internal class
(C<YAML::PP::Preserve::Scalar>) with overloading. If you assign to such
a scalar, the object will be replaced by a simple scalar.

    # assignment, style gets lost
    $styles->[1] .= ' append';

You can also pass C<1> as a value. In this case all preserving options will be
enabled, also if there are new options added in the future.

There are also methods to create preserved nodes from scratch. See the
C<preserved_(scalar|mapping|sequence)> L<"METHODS"> below.

=back

=head2 load_string

    my $doc = $ypp->load_string("foo: bar");
    my @docs = $ypp->load_string("foo: bar\n---\n- a");

Input should be Unicode characters.

So if you read from a file, you should decode it, for example with
C<Encode::decode()>.

Note that in scalar context, C<load_string> and C<load_file> return the first
document (like L<YAML::Syck>), while L<YAML> and L<YAML::XS> return the
last.

=head2 load_file

    my $doc = $ypp->load_file("file.yaml");
    my @docs = $ypp->load_file("file.yaml");

Strings will be loaded as unicode characters.

=head2 dump_string

    my $yaml = $ypp->dump_string($doc);
    my $yaml = $ypp->dump_string($doc1, $doc2);
    my $yaml = $ypp->dump_string(@docs);

Input strings should be Unicode characters.

Output will return Unicode characters.

So if you want to write that to a file (or pass to YAML::XS, for example),
you typically encode it via C<Encode::encode()>.

=head2 dump_file

    $ypp->dump_file("file.yaml", $doc);
    $ypp->dump_file("file.yaml", $doc1, $doc2);
    $ypp->dump_file("file.yaml", @docs);

Input data should be Unicode characters.

=head2 dump

This will dump to a predefined writer. By default it will just use the
L<YAML::PP::Writer> and output a string.

    my $writer = MyWriter->new(\my $output);
    my $yp = YAML::PP->new(
        writer => $writer,
    );
    $yp->dump($data);

=head2 preserved_scalar

Since version 0.024

Experimental. Please report bugs or let me know this is useful and works.

You can define a certain scalar style when dumping data.
Figuring out the best style is a hard task and practically impossible to get
it right for all cases. It's also a matter of taste.

    use YAML::PP::Common qw/ PRESERVE_SCALAR_STYLE YAML_LITERAL_SCALAR_STYLE /;
    my $yp = YAML::PP->new(
        preserve => PRESERVE_SCALAR_STYLE,
    );
    # a single linebreak would normally be dumped with double quotes: "\n"
    my $scalar = $yp->preserved_scalar("\n", style => YAML_LITERAL_SCALAR_STYLE );

    my $data = { literal => $scalar };
    my $dump = $yp->dump_string($data);
    # output
    ---
    literal: |+

    ...


=head2 preserved_mapping, preserved_sequence

Since version 0.024

Experimental. Please report bugs or let me know this is useful and works.

With this you can define which nodes are dumped with the more compact flow
style instead of block style.

If you add C<PRESERVE_ORDER> to the C<preserve> option, it will also keep the
order of the keys in a hash.

    use YAML::PP::Common qw/
        PRESERVE_ORDER PRESERVE_FLOW_STYLE
        YAML_FLOW_MAPPING_STYLE YAML_FLOW_SEQUENCE_STYLE
    /;
    my $yp = YAML::PP->new(
        preserve => PRESERVE_FLOW_STYLE | PRESERVE_ORDER
    );

    my $hash = $yp->preserved_mapping({}, style => YAML_FLOW_MAPPING_STYLE);
    # Add values after initialization to preserve order
    %$hash = (z => 1, a => 2, y => 3, b => 4);

    my $array = $yp->preserved_sequence([23, 24], style => YAML_FLOW_SEQUENCE_STYLE);

    my $data = $yp->preserved_mapping({});
    %$data = ( map => $hash, seq => $array );

    my $dump = $yp->dump_string($data);
    # output
    ---
    map: {z: 1, a: 2, y: 3, b: 4}
    seq: [23, 24]


=head2 loader

Returns or sets the loader object, by default L<YAML::PP::Loader>

=head2 dumper

Returns or sets the dumper object, by default L<YAML::PP::Dumper>

=head2 schema

Returns or sets the schema object

=head2 default_schema

Creates and returns the default schema

=head1 FUNCTIONS

The functions C<Load>, C<LoadFile>, C<Dump> and C<DumpFile> are provided
as a drop-in replacement for other existing YAML processors.
No function is exported by default.

Note that in scalar context, C<Load> and C<LoadFile> return the first
document (like L<YAML::Syck>), while L<YAML> and L<YAML::XS> return the
last.

=over

=item Load

    use YAML::PP qw/ Load /;
    my $doc = Load($yaml);
    my @docs = Load($yaml);

Works like C<load_string>.

=item LoadFile

    use YAML::PP qw/ LoadFile /;
    my $doc = LoadFile($file);
    my @docs = LoadFile($file);
    my @docs = LoadFile($filehandle);

Works like C<load_file>.

=item Dump

    use YAML::PP qw/ Dump /;
    my $yaml = Dump($doc);
    my $yaml = Dump(@docs);

Works like C<dump_string>.

=item DumpFile

    use YAML::PP qw/ DumpFile /;
    DumpFile($file, $doc);
    DumpFile($file, @docs);
    DumpFile($filehandle, @docs);

Works like C<dump_file>.

=back

=head1 PLUGINS

You can alter the behaviour of YAML::PP by using the following schema
classes:

=over

=item L<YAML::PP::Schema::Failsafe>

One of the three YAML 1.2 official schemas

=item L<YAML::PP::Schema::JSON>

One of the three YAML 1.2 official schemas.

=item L<YAML::PP::Schema::Core>

One of the three YAML 1.2 official schemas. Default

=item L<YAML::PP::Schema::YAML1_1>

Schema implementing the most common YAML 1.1 types

=item L<YAML::PP::Schema::Perl>

Serializing Perl objects and types

=item L<YAML::PP::Schema::Binary>

Serializing binary data

=item L<YAML::PP::Schema::Tie::IxHash>

Deprecated. See option C<preserve>

=item L<YAML::PP::Schema::Merge>

YAML 1.1 merge keys for mappings

=item L<YAML::PP::Schema::Include>

Include other YAML files via C<!include> tags

=back

To make the parsing process faster, you can plugin the libyaml parser
with L<YAML::PP::LibYAML>.



=head1 IMPLEMENTATION

The process of loading and dumping is split into the following steps:

    Load:

    YAML Stream        Tokens        Event List        Data Structure
              --------->    --------->        --------->
                lex           parse           construct


    Dump:

    Data Structure       Event List        YAML Stream
                --------->        --------->
                represent           emit


You can dump basic perl types like hashes, arrays, scalars (strings, numbers).
For dumping blessed objects and things like coderefs have a look at
L<YAML::PP::Perl>/L<YAML::PP::Schema::Perl>.

=over

=item L<YAML::PP::Lexer>

The Lexer is reading the YAML stream into tokens. This makes it possible
to generate syntax highlighted YAML output.

Note that the API to retrieve the tokens will change.

=item L<YAML::PP::Parser>

The Parser retrieves the tokens from the Lexer. The main YAML content is then
parsed with the Grammar.

=item L<YAML::PP::Grammar>

=item L<YAML::PP::Constructor>

The Constructor creates a data structure from the Parser events.

=item L<YAML::PP::Loader>

The Loader combines the constructor and parser.

=item L<YAML::PP::Dumper>

The Dumper will delegate to the Representer

=item L<YAML::PP::Representer>

The Representer will create Emitter events from the given data structure.

=item L<YAML::PP::Emitter>

The Emitter creates a YAML stream.

=back

=head2 YAML::PP::Parser

Still TODO:

=over 4

=item Implicit collection keys

    ---
    [ a, b, c ]: value

=item Implicit mapping in flow style sequences

This is supported since 0.029 (except some not relevant cases):

    ---
    [ a, b, c: d ]
    # equals
    [ a, b, { c: d } ]

=item Plain mapping keys ending with colons

    ---
    key ends with two colons::: value

=item Supported Characters

If you have valid YAML that's not parsed, or the other way round, please
create an issue.

=item Line and Column Numbers

You will see line and column numbers in the error message. The column numbers
might still be wrong in some cases.

=item Error Messages

The error messages need to be improved.

=item Unicode Surrogate Pairs

Currently loaded as single characters without validating

=item Possibly more

=back

=head2 YAML::PP::Constructor

The Constructor now supports all three YAML 1.2 Schemas, Failsafe, JSON and
Core.  Additionally you can choose the schema for YAML 1.1 as C<YAML1_1>.

Too see what strings are resolved as booleans, numbers, null etc. look at
L<https://perlpunk.github.io/YAML-PP-p5/schema-examples.html>.

You can choose the Schema like this:

    my $ypp = YAML::PP->new(schema => ['JSON']); # default is 'Core'

The Tags C<!!seq> and C<!!map> are still ignored for now.

It supports:

=over 4

=item Handling of Anchors/Aliases

Like in modules like L<YAML>, the Constructor will use references for mappings and
sequences, but obviously not for scalars.

L<YAML::XS> uses real aliases, which allows also aliasing scalars. I might add
an option for that since aliasing is now available in pure perl.

=item Boolean Handling

You can choose between C<'perl'> (1/'', currently default), C<'JSON::PP'> and
C<'boolean'>.pm for handling boolean types.  That allows you to dump the data
structure with one of the JSON modules without losing information about
booleans.

=item Numbers

Numbers are created as real numbers instead of strings, so that they are
dumped correctly by modules like L<JSON::PP> or L<JSON::XS>, for example.

=item Complex Keys

Mapping Keys in YAML can be more than just scalars. Of course, you can't load
that into a native perl structure. The Constructor will stringify those keys
with L<Data::Dumper> instead of just returning something like
C<HASH(0x55dc1b5d0178)>.

Example:

    use YAML::PP;
    use JSON::PP;
    my $ypp = YAML::PP->new;
    my $coder = JSON::PP->new->ascii->pretty->allow_nonref->canonical;
    my $yaml = <<'EOM';
    complex:
        ?
            ?
                a: 1
                c: 2
            : 23
        : 42
    EOM
    my $data = $yppl->load_string($yaml);
    say $coder->encode($data);
    __END__
    {
       "complex" : {
          "{'{a => 1,c => 2}' => 23}" : 42
       }
    }

=back

TODO:

=over 4

=item Parse Tree

I would like to generate a complete parse tree, that allows you to manipulate
the data structure and also dump it, including all whitespaces and comments.
The spec says that this is throwaway content, but I read that many people
wish to be able to keep the comments.

=back

=head2 YAML::PP::Dumper, YAML::PP::Emitter

The Dumper should be able to dump strings correctly, adding quotes
whenever a plain scalar would look like a special string, like C<true>,
or when it contains or starts with characters that are not allowed.

Most strings will be dumped as plain scalars without quotes. If they
contain special characters or have a special meaning, they will be dumped
with single quotes. If they contain control characters, including <"\n">,
they will be dumped with double quotes.

It will recognize JSON::PP::Boolean and boolean.pm objects and dump them
correctly.

Numbers which also have a C<PV> flag will be recognized as numbers and not
as strings:

    my $int = 23;
    say "int: $int"; # $int will now also have a PV flag

That means that if you accidentally use a string in numeric context, it will
also be recognized as a number:

    my $string = "23";
    my $something = $string + 0;
    print $yp->dump_string($string);
    # will be emitted as an integer without quotes!

The layout is like libyaml output:

    key:
    - a
    - b
    - c
    ---
    - key1: 1
      key2: 2
      key3: 3
    ---
    - - a1
      - a2
    - - b1
      - b2

=head1 WHY

All the available parsers and loaders for Perl are behaving differently,
and more important, aren't conforming to the spec. L<YAML::XS> is
doing pretty well, but C<libyaml> only handles YAML 1.1 and diverges
a bit from the spec. The pure perl loaders lack support for a number of
features.

I was going over L<YAML>.pm issues end of 2016, integrating old patches
from rt.cpan.org and creating some pull requests myself. I realized
that it would be difficult to patch YAML.pm to parse YAML 1.1 or even 1.2,
and it would also break existing usages relying on the current behaviour.


In 2016 Ingy döt Net initiated two really cool projects:

=over 4

=item L<"YAML TEST SUITE">

=item L<"YAML EDITOR">

=back

These projects are a big help for any developer. So I got the idea
to write my own parser and started on New Year's Day 2017.
Without the test suite and the editor I would have never started this.

I also started another YAML Test project which allows one to get a quick
overview of which frameworks support which YAML features:

=over 4

=item L<"YAML TEST MATRIX">

=back

=head2 YAML TEST SUITE

L<https://github.com/yaml/yaml-test-suite>

It contains almost 400 test cases and expected parsing events and more.
There will be more tests coming. This test suite allows you to write parsers
without turning the examples from the Specification into tests yourself.
Also the examples aren't completely covering all cases - the test suite
aims to do that.

The suite contains .tml files, and in a separate 'data' release you will
find the content in separate files, if you can't or don't want to
use TestML.

Thanks also to Felix Krause, who is writing a YAML parser in Nim.
He turned all the spec examples into test cases.

=head2 YAML EDITOR

This is a tool to play around with several YAML parsers and loaders in vim.

L<https://github.com/yaml/yaml-editor>

The project contains the code to build the frameworks (16 as of this
writing) and put it into one big Docker image.

It also contains the yaml-editor itself, which will start a vim in the docker
container. It uses a lot of funky vimscript that makes playing with it easy
and useful. You can choose which frameworks you want to test and see the
output in a grid of vim windows.

Especially when writing a parser it is extremely helpful to have all
the test cases and be able to play around with your own examples to see
how they are handled.

=head2 YAML TEST MATRIX

I was curious to see how the different frameworks handle the test cases,
so, using the test suite and the docker image, I wrote some code that runs
the tests, manipulates the output to compare it with the expected output,
and created a matrix view.

L<https://github.com/perlpunk/yaml-test-matrix>

You can find the latest build at L<https://matrix.yaml.info>

=head1 CONTRIBUTORS

=over

=item Ingy döt Net

Ingy is one of the creators of YAML. In 2016 he started the YAML Test Suite
and the YAML Editor. He also made useful suggestions on the class
hierarchy of YAML::PP.

=item Felix "flyx" Krause

Felix answered countless questions about the YAML Specification.

=back

=head1 SEE ALSO

=over

=item L<YAML>

=item L<YAML::XS>

=item L<YAML::Syck>

=item L<YAML::Tiny>

=item L<YAML::PP::LibYAML>

=item L<YAML::LibYAML::API>

=item L<https://www.yaml.info>

=back

=head1 SPONSORS

The Perl Foundation L<https://www.perlfoundation.org/> sponsored this project
(and the YAML Test Suite) with a grant of 2500 USD in 2017-2018.

=head1 COPYRIGHT AND LICENSE

Copyright 2017-2022 by Tina Müller

This library is free software and may be distributed under the same terms
as perl itself.

=cut
