use strict;
use warnings;
package YAML::PP::Schema;
use B;
use Module::Load qw//;

our $VERSION = '0.034'; # VERSION

use YAML::PP::Common qw/ YAML_PLAIN_SCALAR_STYLE /;

use Scalar::Util qw/ blessed /;

sub new {
    my ($class, %args) = @_;

    my $yaml_version = delete $args{yaml_version};
    my $bool = delete $args{boolean};
    $bool = 'perl' unless defined $bool;
    if (keys %args) {
        die "Unexpected arguments: " . join ', ', sort keys %args;
    }
    my $true;
    my $false;
    my @bool_class;
    my @bools = split m/,/, $bool;
    for my $b (@bools) {
        if ($b eq '*') {
            push @bool_class, ('boolean', 'JSON::PP::Boolean');
            last;
        }
        elsif ($b eq 'JSON::PP') {
            require JSON::PP;
            $true ||= \&_bool_jsonpp_true;
            $false ||= \&_bool_jsonpp_false;
            push @bool_class, 'JSON::PP::Boolean';
        }
        elsif ($b eq 'boolean') {
            require boolean;
            $true ||= \&_bool_booleanpm_true;
            $false ||= \&_bool_booleanpm_false;
            push @bool_class, 'boolean';
        }
        elsif ($b eq 'perl') {
            $true ||= \&_bool_perl_true;
            $false ||= \&_bool_perl_false;
        }
        elsif ($b eq 'perl_experimental') {
            $true ||= \&_bool_perl_true;
            $false ||= \&_bool_perl_false;
            push @bool_class, 'perl_experimental';
        }
        else {
            die "Invalid value for 'boolean': '$b'. Allowed: ('perl', 'boolean', 'JSON::PP')";
        }
    }

    my %representers = (
        'undef' => undef,
        flags => [],
        equals => {},
        regex => [],
        class_equals => {},
        class_matches => [],
        class_isa => [],
        scalarref => undef,
        refref => undef,
        coderef => undef,
        glob => undef,
        tied_equals => {},
    );
    my $self = bless {
        yaml_version => $yaml_version,
        resolvers => {},
        representers => \%representers,
        true => $true,
        false => $false,
        bool_class => \@bool_class,
    }, $class;
    return $self;
}

sub resolvers { return $_[0]->{resolvers} }
sub representers { return $_[0]->{representers} }

sub true { return $_[0]->{true} }
sub false { return $_[0]->{false} }
sub bool_class { return @{ $_[0]->{bool_class} } ? $_[0]->{bool_class} : undef }
sub yaml_version { return $_[0]->{yaml_version} }

my %LOADED_SCHEMA = (
    JSON => 1,
);
my %DEFAULT_SCHEMA = (
    '1.2' => 'Core',
    '1.1' => 'YAML1_1',
);

sub load_subschemas {
    my ($self, @schemas) = @_;
    my $yaml_version = $self->yaml_version;
    my $i = 0;
    while ($i < @schemas) {
        my $item = $schemas[ $i ];
        if ($item eq '+') {
            $item = $DEFAULT_SCHEMA{ $yaml_version };
        }
        $i++;
        if (blessed($item)) {
            $item->register(
                schema => $self,
            );
            next;
        }
        my @options;
        while ($i < @schemas
            and (
                $schemas[ $i ] =~ m/^[^A-Za-z]/
                or
                $schemas[ $i ] =~ m/^[a-zA-Z0-9]+=/
                )
            ) {
            push @options, $schemas[ $i ];
            $i++;
        }

        my $class;
        if ($item =~ m/^\:(.*)/) {
            $class = "$1";
            unless ($class =~ m/\A[A-Za-z0-9_:]+\z/) {
                die "Module name '$class' is invalid";
            }
            Module::Load::load $class;
        }
        else {
            $class = "YAML::PP::Schema::$item";
            unless ($class =~ m/\A[A-Za-z0-9_:]+\z/) {
                die "Module name '$class' is invalid";
            }
            $LOADED_SCHEMA{ $item } ||= Module::Load::load $class;
        }
        $class->register(
            schema => $self,
            options => \@options,
        );

    }
}

sub add_resolver {
    my ($self, %args) = @_;
    my $tag = $args{tag};
    my $rule = $args{match};
    my $resolvers = $self->resolvers;
    my ($type, @rule) = @$rule;
    my $implicit = $args{implicit};
    $implicit = 1 unless defined $implicit;
    my $resolver_list = [];
    if ($tag) {
        if (ref $tag eq 'Regexp') {
            my $res = $resolvers->{tags} ||= [];
            push @$res, [ $tag, {} ];
            push @$resolver_list, $res->[-1]->[1];
        }
        else {
            my $res = $resolvers->{tag}->{ $tag } ||= {};
            push @$resolver_list, $res;
        }
    }
    if ($implicit) {
        push @$resolver_list, $resolvers->{value} ||= {};
    }
    for my $res (@$resolver_list) {
        if ($type eq 'equals') {
            my ($match, $value) = @rule;
            unless (exists $res->{equals}->{ $match }) {
                $res->{equals}->{ $match } = $value;
            }
            next;
        }
        elsif ($type eq 'regex') {
            my ($match, $value) = @rule;
            push @{ $res->{regex} }, [ $match => $value ];
        }
        elsif ($type eq 'all') {
            my ($value) = @rule;
            $res->{all} = $value;
        }
    }
}

sub add_sequence_resolver {
    my ($self, %args) = @_;
    return $self->add_collection_resolver(sequence => %args);
}

sub add_mapping_resolver {
    my ($self, %args) = @_;
    return $self->add_collection_resolver(mapping => %args);
}

sub add_collection_resolver {
    my ($self, $type, %args) = @_;
    my $tag = $args{tag};
    my $implicit = $args{implicit};
    my $resolvers = $self->resolvers;

    if ($tag and ref $tag eq 'Regexp') {
        my $res = $resolvers->{ $type }->{tags} ||= [];
        push @$res, [ $tag, {
            on_create => $args{on_create},
            on_data => $args{on_data},
        } ];
    }
    elsif ($tag) {
        my $res = $resolvers->{ $type }->{tag}->{ $tag } ||= {
            on_create => $args{on_create},
            on_data => $args{on_data},
        };
    }
}

sub add_representer {
    my ($self, %args) = @_;

    my $representers = $self->representers;
    if (my $flags = $args{flags}) {
        my $rep = $representers->{flags};
        push @$rep, \%args;
        return;
    }
    if (my $regex = $args{regex}) {
        my $rep = $representers->{regex};
        push @$rep, \%args;
        return;
    }
    if (my $regex = $args{class_matches}) {
        my $rep = $representers->{class_matches};
        push @$rep, [ $args{class_matches}, $args{code} ];
        return;
    }
    if (my $class_equals = $args{class_equals}) {
        if ($] >= 5.036000 and $class_equals eq 'perl_experimental') {
            $representers->{bool} = {
                code => $args{code},
            };
            return;
        }
        my $rep = $representers->{class_equals};
        $rep->{ $class_equals } = {
            code => $args{code},
        };
        return;
    }
    if (my $class_isa = $args{class_isa}) {
        my $rep = $representers->{class_isa};
        push @$rep, [ $args{class_isa}, $args{code} ];
        return;
    }
    if (my $tied_equals = $args{tied_equals}) {
        my $rep = $representers->{tied_equals};
        $rep->{ $tied_equals } = {
            code => $args{code},
        };
        return;
    }
    if (defined(my $equals = $args{equals})) {
        my $rep = $representers->{equals};
        $rep->{ $equals } = {
            code => $args{code},
        };
        return;
    }
    if (defined(my $scalarref = $args{scalarref})) {
        $representers->{scalarref} = {
            code => $args{code},
        };
        return;
    }
    if (defined(my $refref = $args{refref})) {
        $representers->{refref} = {
            code => $args{code},
        };
        return;
    }
    if (defined(my $coderef = $args{coderef})) {
        $representers->{coderef} = {
            code => $args{code},
        };
        return;
    }
    if (defined(my $glob = $args{glob})) {
        $representers->{glob} = {
            code => $args{code},
        };
        return;
    }
    if (my $undef = $args{undefined}) {
        $representers->{undef} = $undef;
        return;
    }
}

sub load_scalar {
    my ($self, $constructor, $event) = @_;
    my $tag = $event->{tag};
    my $value = $event->{value};

    my $resolvers = $self->resolvers;
    my $res;
    if ($tag) {
        $res = $resolvers->{tag}->{ $tag };
        if (not $res and my $matches = $resolvers->{tags}) {
            for my $match (@$matches) {
                my ($re, $rule) = @$match;
                if ($tag =~ $re) {
                    $res = $rule;
                    last;
                }
            }
        }
    }
    else {
        $res = $resolvers->{value};
        if ($event->{style} ne YAML_PLAIN_SCALAR_STYLE) {
            return $value;
        }
    }

    if (my $equals = $res->{equals}) {
        if (exists $equals->{ $value }) {
            my $res = $equals->{ $value };
            if (ref $res eq 'CODE') {
                return $res->($constructor, $event);
            }
            return $res;
        }
    }
    if (my $regex = $res->{regex}) {
        for my $item (@$regex) {
            my ($re, $sub) = @$item;
            my @matches = $value =~ $re;
            if (@matches) {
                return $sub->($constructor, $event, \@matches);
            }
        }
    }
    if (my $catch_all = $res->{all}) {
        if (ref $catch_all eq 'CODE') {
            return $catch_all->($constructor, $event);
        }
        return $catch_all;
    }
    return $value;
}

sub create_sequence {
    my ($self, $constructor, $event) = @_;
    my $tag = $event->{tag};
    my $data = [];
    my $on_data;

    my $resolvers = $self->resolvers->{sequence};
    if ($tag) {
        if (my $equals = $resolvers->{tag}->{ $tag }) {
            my $on_create = $equals->{on_create};
            $on_data = $equals->{on_data};
            $on_create and $data = $on_create->($constructor, $event);
            return ($data, $on_data);
        }
        if (my $matches = $resolvers->{tags}) {
            for my $match (@$matches) {
                my ($re, $actions) = @$match;
                my $on_create = $actions->{on_create};
                if ($tag =~ $re) {
                    $on_data = $actions->{on_data};
                    $on_create and $data = $on_create->($constructor, $event);
                    return ($data, $on_data);
                }
            }
        }
    }

    return ($data, $on_data);
}

sub create_mapping {
    my ($self, $constructor, $event) = @_;
    my $tag = $event->{tag};
    my $data = {};
    my $on_data;

    my $resolvers = $self->resolvers->{mapping};
    if ($tag) {
        if (my $equals = $resolvers->{tag}->{ $tag }) {
            my $on_create = $equals->{on_create};
            $on_data = $equals->{on_data};
            $on_create and $data = $on_create->($constructor, $event);
            return ($data, $on_data);
        }
        if (my $matches = $resolvers->{tags}) {
            for my $match (@$matches) {
                my ($re, $actions) = @$match;
                my $on_create = $actions->{on_create};
                if ($tag =~ $re) {
                    $on_data = $actions->{on_data};
                    $on_create and $data = $on_create->($constructor, $event);
                    return ($data, $on_data);
                }
            }
        }
    }

    return ($data, $on_data);
}

sub _bool_jsonpp_true { JSON::PP::true() }

sub _bool_booleanpm_true { boolean::true() }

sub _bool_perl_true { !!1 }

sub _bool_jsonpp_false { JSON::PP::false() }

sub _bool_booleanpm_false { boolean::false() }

sub _bool_perl_false { !!0 }

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Schema - Schema for YAML::PP


