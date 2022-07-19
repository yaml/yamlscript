use strict;
use warnings;
package YAML::PP::Representer;

our $VERSION = '0.034'; # VERSION

use Scalar::Util qw/ reftype blessed refaddr /;

use YAML::PP::Common qw/
    YAML_PLAIN_SCALAR_STYLE YAML_SINGLE_QUOTED_SCALAR_STYLE
    YAML_DOUBLE_QUOTED_SCALAR_STYLE
    YAML_ANY_SCALAR_STYLE
    YAML_LITERAL_SCALAR_STYLE YAML_FOLDED_SCALAR_STYLE
    YAML_FLOW_SEQUENCE_STYLE YAML_FLOW_MAPPING_STYLE
    YAML_BLOCK_MAPPING_STYLE YAML_BLOCK_SEQUENCE_STYLE
    PRESERVE_ORDER PRESERVE_SCALAR_STYLE PRESERVE_FLOW_STYLE PRESERVE_ALIAS
/;
use B;

sub new {
    my ($class, %args) = @_;
    my $preserve = delete $args{preserve} || 0;
    if ($preserve == 1) {
        $preserve = PRESERVE_ORDER | PRESERVE_SCALAR_STYLE | PRESERVE_FLOW_STYLE | PRESERVE_ALIAS;
    }
    my $self = bless {
        schema => delete $args{schema},
        preserve => $preserve,
    }, $class;
    if (keys %args) {
        die "Unexpected arguments: " . join ', ', sort keys %args;
    }
    return $self;
}

sub clone {
    my ($self) = @_;
    my $clone = {
        schema => $self->schema,
        preserve => $self->{preserve},
    };
    return bless $clone, ref $self;
}

sub schema { return $_[0]->{schema} }
sub preserve_order { return $_[0]->{preserve} & PRESERVE_ORDER }
sub preserve_scalar_style { return $_[0]->{preserve} & PRESERVE_SCALAR_STYLE }
sub preserve_flow_style { return $_[0]->{preserve} & PRESERVE_FLOW_STYLE }
sub preserve_alias { return $_[0]->{preserve} & PRESERVE_ALIAS }

sub represent_node {
    my ($self, $node) = @_;

    my $preserve_alias = $self->preserve_alias;
    my $preserve_style = $self->preserve_scalar_style;
    if ($preserve_style or $preserve_alias) {
        if (ref $node->{value} eq 'YAML::PP::Preserve::Scalar') {
            my $value = $node->{value}->value;
            if ($preserve_style and $node->{value}->style != YAML_FOLDED_SCALAR_STYLE) {
                $node->{style} = $node->{value}->style;
            }
#            $node->{tag} = $node->{value}->tag;
            $node->{value} = $value;
        }
    }
    $node->{reftype} = reftype($node->{value});
    if (not $node->{reftype} and reftype(\$node->{value}) eq 'GLOB') {
        $node->{reftype} = 'GLOB';
    }

    if ($node->{reftype}) {
        $self->_represent_noderef($node);
    }
    else {
        $self->_represent_node_nonref($node);
    }
    $node->{reftype} = (reftype $node->{data}) || '';

    if ($node->{reftype} eq 'HASH' and my $tied = tied(%{ $node->{data} })) {
        my $representers = $self->schema->representers;
        $tied = ref $tied;
        if (my $def = $representers->{tied_equals}->{ $tied }) {
            my $code = $def->{code};
            my $done = $code->($self, $node);
        }
    }

    if ($node->{reftype} eq 'HASH') {
        unless (defined $node->{items}) {
            # by default we sort hash keys
            my @keys;
            if ($self->preserve_order) {
                @keys = keys %{ $node->{data} };
            }
            else {
                @keys = sort keys %{ $node->{data} };
            }
            for my $key (@keys) {
                push @{ $node->{items} }, $key, $node->{data}->{ $key };
            }
        }
        my %args;
        if ($self->preserve_flow_style and reftype $node->{value} eq 'HASH') {
            if (my $tied = tied %{ $node->{value} } ) {
                $args{style} = $tied->{style};
            }
        }
        return [ mapping => $node, %args ];
    }
    elsif ($node->{reftype} eq 'ARRAY') {
        unless (defined $node->{items}) {
            @{ $node->{items} } = @{ $node->{data} };
        }
        my %args;
        if ($self->preserve_flow_style and reftype $node->{value} eq 'ARRAY') {
            if (my $tied = tied @{ $node->{value} } ) {
                $args{style} = $tied->{style};
            }
        }
        return [ sequence => $node, %args ];
    }
    elsif ($node->{reftype}) {
        die "Cannot handle reftype '$node->{reftype}' (you might want to enable YAML::PP::Schema::Perl)";
    }
    else {
        unless (defined $node->{items}) {
            $node->{items} = [$node->{data}];
        }
        return [ scalar => $node ];
    }

}

my $bool_code = <<'EOM';
sub {
    my ($x) = @_;
    use experimental qw/ builtin /;
    builtin::is_bool($x);
}
EOM
my $is_bool;

sub _represent_node_nonref {
    my ($self, $node) = @_;
    my $representers = $self->schema->representers;

    if (not defined $node->{value}) {
        if (my $undef = $representers->{undef}) {
            return 1 if $undef->($self, $node);
        }
        else {
            $node->{style} = YAML_SINGLE_QUOTED_SCALAR_STYLE;
            $node->{data} = '';
            return 1;
        }
    }
    if ($] >= 5.036000 and my $rep = $representers->{bool}) {
        $is_bool ||= eval $bool_code;
        if ($is_bool->($node->{value})) {
            return $rep->{code}->($self, $node);
        }
    }
    for my $rep (@{ $representers->{flags} }) {
        my $check_flags = $rep->{flags};
        my $flags = B::svref_2object(\$node->{value})->FLAGS;
        if ($flags & $check_flags) {
            return 1 if $rep->{code}->($self, $node);
        }

    }
    if (my $rep = $representers->{equals}->{ $node->{value} }) {
        return 1 if $rep->{code}->($self, $node);
    }
    for my $rep (@{ $representers->{regex} }) {
        if ($node->{value} =~ $rep->{regex}) {
            return 1 if $rep->{code}->($self, $node);
        }
    }
    unless (defined $node->{data}) {
        $node->{data} = $node->{value};
    }
    unless (defined $node->{style}) {
        $node->{style} = YAML_ANY_SCALAR_STYLE;
        $node->{style} = "";
    }
}

sub _represent_noderef {
    my ($self, $node) = @_;
    my $representers = $self->schema->representers;

    if (my $classname = blessed($node->{value})) {
        if (my $def = $representers->{class_equals}->{ $classname }) {
            my $code = $def->{code};
            return 1 if $code->($self, $node);
        }
        for my $matches (@{ $representers->{class_matches} }) {
            my ($re, $code) = @$matches;
            if (ref $re and $classname =~ $re or $re) {
                return 1 if $code->($self, $node);
            }
        }
        for my $isa (@{ $representers->{class_isa} }) {
            my ($class_name, $code) = @$isa;
            if ($node->{ value }->isa($class_name)) {
                return 1 if $code->($self, $node);
            }
        }
    }
    if ($node->{reftype} eq 'SCALAR' and my $scalarref = $representers->{scalarref}) {
        my $code = $scalarref->{code};
        return 1 if $code->($self, $node);
    }
    if ($node->{reftype} eq 'REF' and my $refref = $representers->{refref}) {
        my $code = $refref->{code};
        return 1 if $code->($self, $node);
    }
    if ($node->{reftype} eq 'CODE' and my $coderef = $representers->{coderef}) {
        my $code = $coderef->{code};
        return 1 if $code->($self, $node);
    }
    if ($node->{reftype} eq 'GLOB' and my $glob = $representers->{glob}) {
        my $code = $glob->{code};
        return 1 if $code->($self, $node);
    }
    $node->{data} = $node->{value};

}

1;
