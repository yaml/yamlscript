use strict;
use warnings;
package YAML::PP::Exception;

our $VERSION = '0.034'; # VERSION

use overload '""' => \&to_string;

sub new {
    my ($class, %args) = @_;
    my $self = bless {
        line => $args{line},
        msg => $args{msg},
        next => $args{next},
        where => $args{where},
        yaml => $args{yaml},
        got => $args{got},
        expected => $args{expected},
        column => $args{column},
    }, $class;
    return $self;
}

sub to_string {
    my ($self) = @_;
    my $next = $self->{next};
    my $line = $self->{line};
    my $column = $self->{column};

    my $yaml = '';
    for my $token (@$next) {
        last if $token->{name} eq 'EOL';
        $yaml .= $token->{value};
    }
    $column = '???' unless defined $column;

    my $remaining_yaml = $self->{yaml};
    $remaining_yaml = '' unless defined $remaining_yaml;
    $yaml .= $remaining_yaml;
    {
        local $@; # avoid bug in old Data::Dumper
        require Data::Dumper;
        local $Data::Dumper::Useqq = 1;
        local $Data::Dumper::Terse = 1;
        $yaml = Data::Dumper->Dump([$yaml], ['yaml']);
        chomp $yaml;
    }

    my $lines = 5;
    my @fields;

    if ($self->{got} and $self->{expected}) {
        $lines = 6;
        $line = $self->{got}->{line};
        $column = $self->{got}->{column} + 1;
        @fields = (
            "Line" => $line,
            "Column" => $column,
            "Expected", join(" ", @{ $self->{expected} }),
            "Got", $self->{got}->{name},
            "Where", $self->{where},
            "YAML", $yaml,
        );
    }
    else {
        @fields = (
            "Line" => $line,
            "Column" => $column,
            "Message", $self->{msg},
            "Where", $self->{where},
            "YAML", $yaml,
        );
    }
    my $fmt = join "\n", ("%-10s: %s") x $lines;
    my $string = sprintf $fmt, @fields;
    return $string;
}

1;
