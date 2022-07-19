use strict;
use warnings;
package YAML::PP::Schema::Perl;

our $VERSION = '0.034'; # VERSION

use Scalar::Util qw/ blessed reftype /;

my $qr_prefix;
# workaround to avoid growing regexes when repeatedly loading and dumping
# e.g. (?^:(?^:regex))
{
    $qr_prefix = qr{\(\?-xism\:};
    if ($] >= 5.014) {
        $qr_prefix = qr{\(\?\^(?:[uadl])?\:};
    }
}

sub new {
    my ($class, %args) = @_;
    my $tags = $args{tags} || [];
    my $loadcode = $args{loadcode};
    $loadcode ||= 0;
    my $classes = $args{classes};

    my $self = bless {
        tags => $tags,
        loadcode => $loadcode,
        classes => $classes,
    }, $class;
}

sub register {
    my ($self, %args) = @_;
    my $schema = $args{schema};

    my $tags;
    my $loadcode = 0;
    my $classes;
    if (blessed($self)) {
        $tags = $self->{tags};
        @$tags = ('!perl') unless @$tags;
        $loadcode = $self->{loadcode};
        $classes = $self->{classes};
    }
    else {
        my $options = $args{options};
        my $tagtype = '!perl';
        for my $option (@$options) {
            if ($option =~ m/^tags?=(.+)$/) {
                $tagtype = $1;
            }
            elsif ($option eq '+loadcode') {
                $loadcode = 1;
            }
        }
        $tags = [split m/\+/, $tagtype];
    }


    my $perl_tag;
    my %tagtypes;
    my @perl_tags;
    for my $type (@$tags) {
        if ($type eq '!perl') {
            $perl_tag ||= $type;
            push @perl_tags, '!perl';
        }
        elsif ($type eq '!!perl') {
            $perl_tag ||= 'tag:yaml.org,2002:perl';
            push @perl_tags, 'tag:yaml.org,2002:perl';
        }
        else {
            die "Invalid tagtype '$type'";
        }
        $tagtypes{ $type } = 1;
    }

    my $perl_regex = '!perl';
    if ($tagtypes{'!perl'} and $tagtypes{'!!perl'}) {
        $perl_regex = '(?:tag:yaml\\.org,2002:|!)perl';
    }
    elsif ($tagtypes{'!perl'}) {
        $perl_regex = '!perl';
    }
    elsif ($tagtypes{'!!perl'}) {
        $perl_regex = 'tag:yaml\\.org,2002:perl';
    }

    my $class_regex = qr{.+};
    my $no_objects = 0;
    if ($classes) {
        if (@$classes) {
            $class_regex = '(' . join( '|', map "\Q$_\E", @$classes ) . ')';
        }
        else {
            $no_objects = 1;
            $class_regex = '';
        }
    }

    # Code
    if ($loadcode) {
        my $load_code = sub {
            my ($constructor, $event) = @_;
            return $self->evaluate_code($event->{value});
        };
        my $load_code_blessed = sub {
            my ($constructor, $event) = @_;
            my $class = $event->{tag};
            $class =~ s{^$perl_regex/code:}{};
            my $sub = $self->evaluate_code($event->{value});
            return $self->object($sub, $class);
        };
        $schema->add_resolver(
            tag => "$_/code",
            match => [ all => $load_code],
            implicit => 0,
        ) for @perl_tags;
        $schema->add_resolver(
            tag => qr{^$perl_regex/code:$class_regex$},
            match => [ all => $load_code_blessed ],
            implicit => 0,
        );
        $schema->add_resolver(
            tag => qr{^$perl_regex/code:.+},
            match => [ all => $load_code ],
            implicit => 0,
        ) if $no_objects;
    }
    else {
        my $loadcode_dummy = sub { return sub {} };
        my $loadcode_blessed_dummy = sub {
            my ($constructor, $event) = @_;
            my $class = $event->{tag};
            $class =~ s{^$perl_regex/code:}{};
            return $self->object(sub {}, $class);
        };
        $schema->add_resolver(
            tag => "$_/code",
            match => [ all => $loadcode_dummy ],
            implicit => 0,
        ) for @perl_tags;
        $schema->add_resolver(
            tag => qr{^$perl_regex/code:$class_regex$},
            match => [ all => $loadcode_blessed_dummy ],
            implicit => 0,
        );
        $schema->add_resolver(
            tag => qr{^$perl_regex/code:.+},
            match => [ all => $loadcode_dummy ],
            implicit => 0,
        ) if $no_objects;
    }

    # Glob
    my $load_glob = sub {
        my $value = undef;
        return \$value;
    };
    my $load_glob_blessed = sub {
        my ($constructor, $event) = @_;
        my $class = $event->{tag};
        $class =~ s{^$perl_regex/glob:}{};
        my $value = undef;
        return $self->object(\$value, $class);
    };

    $schema->add_mapping_resolver(
        tag => "$_/glob",
        on_create => $load_glob,
        on_data => sub {
            my ($constructor, $ref, $list) = @_;
            $$ref = $self->construct_glob($list);
        },
    ) for @perl_tags;
    if ($no_objects) {
        $schema->add_mapping_resolver(
            tag => qr{^$perl_regex/glob:.+$},
            on_create => $load_glob,
            on_data => sub {
                my ($constructor, $ref, $list) = @_;
                $$ref = $self->construct_glob($list);
            },
        );
    }
    else {
        $schema->add_mapping_resolver(
            tag => qr{^$perl_regex/glob:$class_regex$},
            on_create => $load_glob_blessed,
            on_data => sub {
                my ($constructor, $ref, $list) = @_;
                $$$ref = $self->construct_glob($list);
            },
        );
    }

    # Regex
    my $load_regex = sub {
        my ($constructor, $event) = @_;
        return $self->construct_regex($event->{value});
    };
    my $load_regex_blessed = sub {
        my ($constructor, $event) = @_;
        my $class = $event->{tag};
        $class =~ s{^$perl_regex/regexp:}{};
        my $qr = $self->construct_regex($event->{value});
        return $self->object($qr, $class);
    };
    $schema->add_resolver(
        tag => "$_/regexp",
        match => [ all => $load_regex ],
        implicit => 0,
    ) for @perl_tags;
    $schema->add_resolver(
        tag => qr{^$perl_regex/regexp:$class_regex$},
        match => [ all => $load_regex_blessed ],
        implicit => 0,
    );
    $schema->add_resolver(
        tag => qr{^$perl_regex/regexp:$class_regex$},
        match => [ all => $load_regex ],
        implicit => 0,
    ) if $no_objects;

    my $load_sequence = sub { return [] };
    my $load_sequence_blessed = sub {
        my ($constructor, $event) = @_;
        my $class = $event->{tag};
        $class =~ s{^$perl_regex/array:}{};
        return $self->object([], $class);
    };
    $schema->add_sequence_resolver(
        tag => "$_/array",
        on_create => $load_sequence,
    ) for @perl_tags;
    $schema->add_sequence_resolver(
        tag => qr{^$perl_regex/array:$class_regex$},
        on_create => $load_sequence_blessed,
    );
    $schema->add_sequence_resolver(
        tag => qr{^$perl_regex/array:.+$},
        on_create => $load_sequence,
    ) if $no_objects;

    my $load_mapping = sub { return {} };
    my $load_mapping_blessed = sub {
        my ($constructor, $event) = @_;
        my $class = $event->{tag};
        $class =~ s{^$perl_regex/hash:}{};
        return $self->object({}, $class);
    };
    $schema->add_mapping_resolver(
        tag => "$_/hash",
        on_create => $load_mapping,
    ) for @perl_tags;
    $schema->add_mapping_resolver(
        tag => qr{^$perl_regex/hash:$class_regex$},
        on_create => $load_mapping_blessed,
    );
    $schema->add_mapping_resolver(
        tag => qr{^$perl_regex/hash:.+$},
        on_create => $load_mapping,
    ) if $no_objects;

    # Ref
    my $load_ref = sub {
        my $value = undef;
        return \$value;
    };
    my $load_ref_blessed = sub {
        my ($constructor, $event) = @_;
        my $class = $event->{tag};
        $class =~ s{^$perl_regex/ref:}{};
        my $value = undef;
        return $self->object(\$value, $class);
    };
    $schema->add_mapping_resolver(
        tag => "$_/ref",
        on_create => $load_ref,
        on_data => sub {
            my ($constructor, $ref, $list) = @_;
            $$$ref = $self->construct_ref($list);
        },
    ) for @perl_tags;
    $schema->add_mapping_resolver(
        tag => qr{^$perl_regex/ref:$class_regex$},
        on_create => $load_ref_blessed,
        on_data => sub {
            my ($constructor, $ref, $list) = @_;
            $$$ref = $self->construct_ref($list);
        },
    );
    $schema->add_mapping_resolver(
        tag => qr{^$perl_regex/ref:.+$},
        on_create => $load_ref,
        on_data => sub {
            my ($constructor, $ref, $list) = @_;
            $$$ref = $self->construct_ref($list);
        },
    ) if $no_objects;

    # Scalar ref
    my $load_scalar_ref = sub {
        my $value = undef;
        return \$value;
    };
    my $load_scalar_ref_blessed = sub {
        my ($constructor, $event) = @_;
        my $class = $event->{tag};
        $class =~ s{^$perl_regex/scalar:}{};
        my $value = undef;
        return $self->object(\$value, $class);
    };
    $schema->add_mapping_resolver(
        tag => "$_/scalar",
        on_create => $load_scalar_ref,
        on_data => sub {
            my ($constructor, $ref, $list) = @_;
            $$$ref = $self->construct_scalar($list);
        },
    ) for @perl_tags;
    $schema->add_mapping_resolver(
        tag => qr{^$perl_regex/scalar:$class_regex$},
        on_create => $load_scalar_ref_blessed,
        on_data => sub {
            my ($constructor, $ref, $list) = @_;
            $$$ref = $self->construct_scalar($list);
        },
    );
    $schema->add_mapping_resolver(
        tag => qr{^$perl_regex/scalar:.+$},
        on_create => $load_scalar_ref,
        on_data => sub {
            my ($constructor, $ref, $list) = @_;
            $$$ref = $self->construct_scalar($list);
        },
    ) if $no_objects;

    $schema->add_representer(
        scalarref => 1,
        code => sub {
            my ($rep, $node) = @_;
            $node->{tag} = $perl_tag . "/scalar";
            $node->{data} = $self->represent_scalar($node->{value});
        },
    );
    $schema->add_representer(
        refref => 1,
        code => sub {
            my ($rep, $node) = @_;
            $node->{tag} = $perl_tag . "/ref";
            $node->{data} = $self->represent_ref($node->{value});
        },
    );
    $schema->add_representer(
        coderef => 1,
        code => sub {
            my ($rep, $node) = @_;
            $node->{tag} = $perl_tag . "/code";
            $node->{data} = $self->represent_code($node->{value});
        },
    );
    $schema->add_representer(
        glob => 1,
        code => sub {
            my ($rep, $node) = @_;
            $node->{tag} = $perl_tag . "/glob";
            $node->{data} = $self->represent_glob($node->{value});
        },
    );

    $schema->add_representer(
        class_matches => 1,
        code => sub {
            my ($rep, $node) = @_;
            my $blessed = blessed $node->{value};
            my $tag_blessed = ":$blessed";
            if ($blessed !~ m/^$class_regex$/) {
                $tag_blessed = '';
            }
            $node->{tag} = sprintf "$perl_tag/%s%s",
                lc($node->{reftype}), $tag_blessed;
            if ($node->{reftype} eq 'HASH') {
                $node->{data} = $node->{value};
            }
            elsif ($node->{reftype} eq 'ARRAY') {
                $node->{data} = $node->{value};
            }

            # Fun with regexes in perl versions!
            elsif ($node->{reftype} eq 'REGEXP') {
                if ($blessed eq 'Regexp') {
                    $node->{tag} = $perl_tag . "/regexp";
                }
                $node->{data} = $self->represent_regex($node->{value});
            }
            elsif ($node->{reftype} eq 'SCALAR') {

                # in perl <= 5.10 regex reftype(regex) was SCALAR
                if ($blessed eq 'Regexp') {
                    $node->{tag} = $perl_tag . '/regexp';
                    $node->{data} = $self->represent_regex($node->{value});
                }

                # In perl <= 5.10 there seemed to be no better pure perl
                # way to detect a blessed regex?
                elsif (
                    $] <= 5.010001
                    and not defined ${ $node->{value} }
                    and $node->{value} =~ m/^\(\?/
                ) {
                    $node->{tag} = $perl_tag . '/regexp' . $tag_blessed;
                    $node->{data} = $self->represent_regex($node->{value});
                }
                else {
                    # phew, just a simple scalarref
                    $node->{data} = $self->represent_scalar($node->{value});
                }
            }
            elsif ($node->{reftype} eq 'REF') {
                $node->{data} = $self->represent_ref($node->{value});
            }

            elsif ($node->{reftype} eq 'CODE') {
                $node->{data} = $self->represent_code($node->{value});
            }
            elsif ($node->{reftype} eq 'GLOB') {
                $node->{data} = $self->represent_glob($node->{value});
            }
            else {
                die "Reftype '$node->{reftype}' not implemented";
            }

            return 1;
        },
    );
    return;
}

sub evaluate_code {
    my ($self, $code) = @_;
    unless ($code =~ m/^ \s* \{ .* \} \s* \z/xs) {
        die "Malformed code";
    }
    $code = "sub $code";
    my $sub = eval $code;
    if ($@) {
        die "Couldn't eval code: $@>>$code<<";
    }
    return $sub;
}

sub construct_regex {
    my ($self, $regex) = @_;
    if ($regex =~ m/^$qr_prefix(.*)\)\z/s) {
        $regex = $1;
    }
    my $qr = qr{$regex};
    return $qr;
}

sub construct_glob {
    my ($self, $list) = @_;
    if (@$list % 2) {
        die "Unexpected data in perl/glob construction";
    }
    my %globdata = @$list;
    my $name = delete $globdata{NAME} or die "Missing NAME in perl/glob";
    my $pkg = delete $globdata{PACKAGE};
    $pkg = 'main' unless defined $pkg;
    my @allowed = qw(SCALAR ARRAY HASH CODE IO);
    delete @globdata{ @allowed };
    if (my @keys = keys %globdata) {
        die "Unexpected keys in perl/glob: @keys";
    }
    no strict 'refs';
    return *{"${pkg}::$name"};
}

sub construct_scalar {
    my ($self, $list) = @_;
    if (@$list != 2) {
        die "Unexpected data in perl/scalar construction";
    }
    my ($key, $value) = @$list;
    unless ($key eq '=') {
        die "Unexpected data in perl/scalar construction";
    }
    return $value;
}

sub construct_ref {
    &construct_scalar;
}

sub represent_scalar {
    my ($self, $value) = @_;
    return { '=' => $$value };
}

sub represent_ref {
    &represent_scalar;
}

sub represent_code {
    my ($self, $code) = @_;
    require B::Deparse;
    my $deparse = B::Deparse->new("-p", "-sC");
    return $deparse->coderef2text($code);
}


my @stats = qw/ device inode mode links uid gid rdev size
    atime mtime ctime blksize blocks /;
sub represent_glob {
    my ($self, $glob) = @_;
    my %glob;
    for my $type (qw/ PACKAGE NAME SCALAR ARRAY HASH CODE IO /) {
        my $value = *{ $glob }{ $type };
        if ($type eq 'SCALAR') {
            $value = $$value;
        }
        elsif ($type eq 'IO') {
            if (defined $value) {
                undef $value;
                $value->{stat} = {};
                if ($value->{fileno} = fileno(*{ $glob })) {
                    @{ $value->{stat} }{ @stats } = stat(*{ $glob });
                    $value->{tell} = tell *{ $glob };
                }
            }
        }
        $glob{ $type } = $value if defined $value;
    }
    return \%glob;
}

sub represent_regex {
    my ($self, $regex) = @_;
    $regex = "$regex";
    if ($regex =~ m/^$qr_prefix(.*)\)\z/s) {
        $regex = $1;
    }
    return $regex;
}

sub object {
    my ($self, $data, $class) = @_;
    return bless $data, $class;
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Schema::Perl - Schema for serializing perl objects and special types

=head1 SYNOPSIS

    use YAML::PP;
    # This can be dangerous when loading untrusted YAML!
    my $yp = YAML::PP->new( schema => [qw/ + Perl /] );
    # or
    my $yp = YAML::PP->new( schema => [qw/ Core Perl /] );
    my $yaml = $yp->dump_string(sub { return 23 });

    # loading code references
    # This is very dangerous when loading untrusted YAML!!
    my $yp = YAML::PP->new( schema => [qw/ + Perl +loadcode /] );
    my $code = $yp->load_string(<<'EOM');
    --- !perl/code |
        {
            use 5.010;
            my ($name) = @_;
            say "Hello $name!";
        }
    EOM
    $code->("Ingy");

=head1 DESCRIPTION

This schema allows you to load and dump perl objects and special types.

Please note that loading objects of arbitrary classes can be dangerous
in Perl. You have to load the modules yourself, but if an exploitable module
is loaded and an object is created, its C<DESTROY> method will be called
when the object falls out of scope. L<File::Temp> is an example that can
be exploitable and might remove arbitrary files.

Dumping code references is on by default, but not loading (because that is
easily exploitable since it's using string C<eval>).

=head2 Tag Styles

You can define the style of tags you want to support:

    my $yp_perl_two_one = YAML::PP->new(
        schema => [qw/ + Perl tags=!!perl+!perl /],
    );

=over

=item C<!perl> (default)

Only C<!perl/type> tags are supported.

=item C<!!perl>

Only C<!!perl/type> tags are supported.

=item C<!perl+!!perl>

Both C<!perl/type> and C<!!perl/tag> are supported when loading. When dumping,
C<!perl/type> is used.

=item C<!!perl+!perl>

Both C<!perl/type> and C<!!perl/tag> are supported when loading. When dumping,
C<!!perl/type> is used.

=back

L<YAML>.pm, L<YAML::Syck> and L<YAML::XS> are using C<!!perl/type> when dumping.

L<YAML>.pm and L<YAML::Syck> are supporting both C<!perl/type> and
C<!!perl/type> when loading. L<YAML::XS> currently only supports the latter.

=head2 Allow only certain classes

Since v0.017

Blessing arbitrary objects can be dangerous.  Maybe you want to allow blessing
only specific classes and ignore others.  For this you have to instantiate
a Perl Schema object first and use the C<classes> option.

Currently it only allows a list of strings:

    my $perl = YAML::PP::Schema::Perl->new(
        classes => ['Foo', 'Bar'],
    );
    my $yp = YAML::PP::Perl->new(
        schema => [qw/ + /, $perl],
    );

Allowed classes will be loaded and dumped as usual. The others will be ignored.

If you want to allow no objects at all, pass an empty array ref.


=cut

=head2 EXAMPLES

This is a list of the currently supported types and how they are dumped into
YAML:

=cut

### BEGIN EXAMPLE

=pod

=over 4

=item array

        # Code
        [
            qw/ one two three four /
        ]


        # YAML
        ---
        - one
        - two
        - three
        - four


=item array_blessed

        # Code
        bless [
            qw/ one two three four /
        ], "Just::An::Arrayref"


        # YAML
        --- !perl/array:Just::An::Arrayref
        - one
        - two
        - three
        - four


=item circular

        # Code
        my $circle = bless [ 1, 2 ], 'Circle';
        push @$circle, $circle;
        $circle;


        # YAML
        --- &1 !perl/array:Circle
        - 1
        - 2
        - *1


=item coderef

        # Code
        sub {
            my (%args) = @_;
            return $args{x} + $args{y};
        }


        # YAML
        --- !perl/code |-
          {
              use warnings;
              use strict;
              (my(%args) = @_);
              (return ($args{'x'} + $args{'y'}));
          }


=item coderef_blessed

        # Code
        bless sub {
            my (%args) = @_;
            return $args{x} - $args{y};
        }, "I::Am::Code"


        # YAML
        --- !perl/code:I::Am::Code |-
          {
              use warnings;
              use strict;
              (my(%args) = @_);
              (return ($args{'x'} - $args{'y'}));
          }


=item hash

        # Code
        {
            U => 2,
            B => 52,
        }


        # YAML
        ---
        B: 52
        U: 2


=item hash_blessed

        # Code
        bless {
            U => 2,
            B => 52,
        }, 'A::Very::Exclusive::Class'


        # YAML
        --- !perl/hash:A::Very::Exclusive::Class
        B: 52
        U: 2


=item refref

        # Code
        my $ref = { a => 'hash' };
        my $refref = \$ref;
        $refref;


        # YAML
        --- !perl/ref
        =:
          a: hash


=item refref_blessed

        # Code
        my $ref = { a => 'hash' };
        my $refref = bless \$ref, 'Foo';
        $refref;


        # YAML
        --- !perl/ref:Foo
        =:
          a: hash


=item regexp

        # Code
        my $string = 'unblessed';
        qr{$string}


        # YAML
        --- !perl/regexp unblessed


=item regexp_blessed

        # Code
        my $string = 'blessed';
        bless qr{$string}, "Foo"


        # YAML
        --- !perl/regexp:Foo blessed


=item scalarref

        # Code
        my $scalar = "some string";
        my $scalarref = \$scalar;
        $scalarref;


        # YAML
        --- !perl/scalar
        =: some string


=item scalarref_blessed

        # Code
        my $scalar = "some other string";
        my $scalarref = bless \$scalar, 'Foo';
        $scalarref;


        # YAML
        --- !perl/scalar:Foo
        =: some other string




=back

=cut

### END EXAMPLE

=head2 METHODS

=over

=item new

    my $perl = YAML::PP::Schema::Perl->new(
        tags => "!perl",
        classes => ['MyClass'],
        loadcode => 1,
    );

The constructor recognizes the following options:

=over

=item tags

Default: 'C<!perl>'

See L<"Tag Styles">

=item classes

Default: C<undef>

Since: v0.017

Accepts an array ref of class names

=item loadcode

Default: 0

=back

=item register

A class method called by L<YAML::PP::Schema>

=item construct_ref, represent_ref

Perl variables of the type C<REF> are represented in yaml like this:

    --- !perl/ref
    =:
      a: 1

C<construct_ref> returns the perl data:

    my $data = YAML::PP::Schema::Perl->construct_ref([ '=', { some => 'data' } );
    my $data = \{ a => 1 };

C<represent_ref> turns a C<REF> variable into a YAML mapping:

    my $data = YAML::PP::Schema::Perl->represent_ref(\{ a => 1 });
    my $data = { '=' => { a => 1 } };

=item construct_scalar, represent_scalar

Perl variables of the type C<SCALAR> are represented in yaml like this:

    --- !perl/scalar
    =: string

C<construct_scalar> returns the perl data:

    my $data = YAML::PP::Schema::Perl->construct_ref([ '=', 'string' );
    my $data = \'string';

C<represent_scalar> turns a C<SCALAR> variable into a YAML mapping:

    my $data = YAML::PP::Schema::Perl->represent_scalar(\'string');
    my $data = { '=' => 'string' };

=item construct_regex, represent_regex

C<construct_regex> returns a C<qr{}> object from the YAML string:

    my $qr = YAML::PP::Schema::Perl->construct_regex('foo.*');

C<represent_regex> returns a string representing the regex object:

    my $string = YAML::PP::Schema::Perl->represent_regex(qr{...});

=item evaluate_code, represent_code

C<evaluate_code> returns a code reference from a string. The string must
start with a C<{> and end with a C<}>.

    my $code = YAML::PP::Schema::Perl->evaluate_code('{ return 23 }');

C<represent_code> returns a string representation of the code reference
with the help of B::Deparse:

    my $string = YAML::PP::Schema::Perl->represent_code(sub { return 23 });

=item construct_glob, represent_glob

C<construct_glob> returns a glob from a hash.

    my $glob = YAML::PP::Schema::Perl->construct_glob($hash);

C<represent_glob> returns a hash representation of the glob.

    my $hash = YAML::PP::Schema::Perl->represent_glob($glob);

=item object

Does the same as C<bless>:

    my $object = YAML::PP::Schema::Perl->object($data, $class);

=back

=cut
