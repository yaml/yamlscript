package YAMLScript::Compiler;
use Mo qw(default xxx);

has yaml => ();
has from => ();
has code => ();
has need => ();
has loader => ();
has ns => ();

use YAMLScript::NS;
use YAMLScript::Expr;
use YAMLScript::Func;
use YAMLScript::Str;
use YAMLScript::Util;

use YAML::PP;
use YAML::PP::Schema;

# Apply patches for YAML::PP::Schema
BEGIN {
    my $create_mapping = \&YAML::PP::Schema::create_mapping;
    my $create_sequence = \&YAML::PP::Schema::create_sequence;
    my $load_scalar = \&YAML::PP::Schema::load_scalar;
    no warnings 'redefine';
    *YAML::PP::Schema::create_mapping = sub {
        $_[2]->{tag} //= '!';
        goto $create_mapping;
    };
    *YAML::PP::Schema::create_sequence = sub {
        $_[2]->{tag} //= '!';
        goto $create_sequence;
    };
    *YAML::PP::Schema::load_scalar = sub {
        $_[2]->{tag} //= '!'
            if $_[2]->{style} == 1;
        goto $load_scalar;
    };
}

# Regex patterns for YAMLScript DSL syntax:
my $lc = qr/(?:[a-z])/;             # lower case
my $dg = qr/(?:[0-9])/;             # digit
my $an = qr/(?:[a-z0-9])/;          # alphanum
my $sp = qr/(?:[-])/;               # separator
my $p1 = qr/(?:$lc$an*)/;           # part 1 of identifier
my $pt = qr/(?:$an+)/;              # other part of identifier
my $id = qr/(?:_|$p1(?:$sp$pt)*)/;  # identifier

my $punc = qr/(?:[\-\+\*\/\.\=\<\>\:])/;

my $key_defn = qr/^($id)\((.*)\)$/;
my %exprs = (
    def  => qr/^($id)\ *=$/,
    defn => $key_defn,
    op   => qr/^\(($punc+)\)$/,
    call => qr/^($id)$/,
);

# Compile ->yaml to ->code using YAML::PP custom construction
sub compile {
    my ($self) = @_;

    # Create a YAML loader object with an empty schema:
    my $loader = $self->{loader} =
        YAML::PP->new(
            schema => ['Failsafe'],
        );

    # Set up the custom YAML loader:
    $self->configure;

    # Get the input YAMLScript (YAML string):
    my $yaml = $self->yaml;

    # All the compilation happens in the loader:
    my $code = $self->{code} =
        $loader->load_string($yaml);

    # Make a new NS (namespace) object:
    my $need = $self->need;
    unshift @$need, 'YS-Core';
    my $ns = YAMLScript::NS->new(
        NEED => $need,
    );

    while (my ($key, $val) = each %$code) {
        if ($key eq 'use') {
            $val = [ $val ] unless ref($val) eq 'ARRAY';
            push @$need, @$val;
        }
        else {
            $key =~ $key_defn or
                die "Invalid key '$key' in top level of '${\$self->from}'";
            my $name = $1;
            my $sign = $2;
            $sign = [ split /\s*,\s*/, $sign ];
            my $func = YAMLScript::Func->new(
                ____ => $name,
                sign => $sign,
                body => $val,
            );
            my $arity = @$sign;
            my $full = "${name}__$arity";
            $full =~ s/-/_/g;

            $ns->{$full} = sub {
                YAMLScript::Call->new(
                    ____ => $full,
                    code => $func,
                    args => $_[0],
                ),
            };
        }
    }

    $ns->NS_init;

    # Return the NS object:
    return $ns;
}

# Configure the YAML loader with custom constructors:
sub configure {
    my ($self) = @_;

    my $loader = $self->loader;
    my $schema = $loader->schema;

    $schema->add_mapping_resolver(
        tag => qr/^/,
        on_create => sub {
            my ($constructor, $event) = @_;
            {};
        },
        on_data => sub {
            my ($constructor, $ref, $data) = @_;
            my $hash = $$ref;
            for (my $i = 0; $i < @$data; $i += 2) {
                my ($key, $val) = @$data[$i, $i+1];
                $key = $$key if ref($key) eq 'YAMLScript::Str';
                if (ref($val) eq 'YAMLScript::Str') {
                    if ($$val !~ /\$$id/) {
                        $val = $$val;
                    }
                }
                $hash->{$key} = $val;
            }
            if (@$data == 2) {
                my ($key, $val) = @$data;
                if (ref($key) eq 'YAMLScript::Str' and
                    $$key !~ $key_defn
                ) {
                    $key = $$key;
                    $val = delete $hash->{$key};
                    # YAMLScript 'def' (assignment)
                    if ($key =~ /^($id)\s*=$/) {
                        $hash->{____} = 'def';
                        $hash->{args} = [$1, $val];
                    }
                    # YAMLScript 'defn' (function definition)
                    elsif ($key =~ /^([-\w]+)\((.*)\)$/) {
                        my ($name, $sign) = ($1, $2);
                        $sign = [ split /\s*,\s*/, $sign ];
                        $val = [ $val ] unless ref($val) eq 'ARRAY';
                        my $func = bless {
                            ____ => $name,
                            sign => $sign,
                            body => $val,
                        }, 'YAMLScript::Func';
                        $hash->{____} = 'defn';
                        $hash->{args} = $func;
                    }
                    else {
                        $hash->{____} = $key;
                        $val = [ $val ] unless ref($val) eq 'ARRAY';
                        $hash->{args} = $val;
                    }
                    bless $hash, 'YAMLScript::Expr';
                }
            }
            return;
        },
    );

    $schema->add_sequence_resolver(
        tag => qr/^/,
        on_create => sub {
            my $tag = $_[1]->{tag};
            if ($tag eq '!') {
                return [];
            }
            else {
                return {____ => substr($tag, 1)};
            }
        },
        on_data => sub {
            my ($constructor, $ref, $data) = @_;
            if (ref($$ref) eq 'HASH') {
                my $hash = bless $$ref, 'YAMLScript::Expr';
                my $args = [
                    map {
                        if (ref eq 'YAMLScript::Str') {
                            if ($$_ !~ /\$$id/) {
                                $_ = $$_;
                            }
                        }
                        $_;
                    } @$data
                ];
                $hash->{args} = $args;
            }
            else {
                my $array = $$ref;
                for my $val (@$data) {
                    if (ref($val) eq 'YAMLScript::Str') {
                        if ($$val !~ /\$$id/) {
                            $val = $$val;
                        }
                    }
                    push @$array, $val;
                }
            }
        },
    );

    my $re_num = qr/^-?\d+$/;
    $schema->add_resolver(
        tag => '!',
        match => [
            all => sub {
                my ($constructor, $event) = @_;
                my $value = $event->{value};
                if ($value eq '') {
                    return undef; # XXX maybe YAMLScript::Nil?
                }
                if ($value =~ $re_num) {
                    $value += 0;
                    return $value;
                }
                return bless \$value, 'YAMLScript::Str';
            },
        ],
    );
}
