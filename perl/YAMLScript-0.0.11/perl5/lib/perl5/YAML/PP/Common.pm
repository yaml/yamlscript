use strict;
use warnings;
package YAML::PP::Common;

our $VERSION = '0.034'; # VERSION

use base 'Exporter';

my @p = qw/
    PRESERVE_ALL PRESERVE_ORDER PRESERVE_SCALAR_STYLE PRESERVE_FLOW_STYLE
    PRESERVE_ALIAS
/;
my @s = qw/
    YAML_ANY_SCALAR_STYLE YAML_PLAIN_SCALAR_STYLE
    YAML_SINGLE_QUOTED_SCALAR_STYLE YAML_DOUBLE_QUOTED_SCALAR_STYLE
    YAML_LITERAL_SCALAR_STYLE YAML_FOLDED_SCALAR_STYLE
    YAML_QUOTED_SCALAR_STYLE

    YAML_ANY_SEQUENCE_STYLE
    YAML_BLOCK_SEQUENCE_STYLE YAML_FLOW_SEQUENCE_STYLE

    YAML_ANY_MAPPING_STYLE
    YAML_BLOCK_MAPPING_STYLE YAML_FLOW_MAPPING_STYLE
/;
our @EXPORT_OK = (@s, @p);

our %EXPORT_TAGS = (
    PRESERVE => [@p],
    STYLES => [@s],
);

use constant {
    YAML_ANY_SCALAR_STYLE           => 0,
    YAML_PLAIN_SCALAR_STYLE         => 1,
    YAML_SINGLE_QUOTED_SCALAR_STYLE => 2,
    YAML_DOUBLE_QUOTED_SCALAR_STYLE => 3,
    YAML_LITERAL_SCALAR_STYLE       => 4,
    YAML_FOLDED_SCALAR_STYLE        => 5,
    YAML_QUOTED_SCALAR_STYLE        => 'Q', # deprecated

    YAML_ANY_SEQUENCE_STYLE   => 0,
    YAML_BLOCK_SEQUENCE_STYLE => 1,
    YAML_FLOW_SEQUENCE_STYLE  => 2,

    YAML_ANY_MAPPING_STYLE   => 0,
    YAML_BLOCK_MAPPING_STYLE => 1,
    YAML_FLOW_MAPPING_STYLE  => 2,

    PRESERVE_ORDER        => 2,
    PRESERVE_SCALAR_STYLE => 4,
    PRESERVE_FLOW_STYLE   => 8,
    PRESERVE_ALIAS        => 16,

    PRESERVE_ALL          => 31,
};

my %scalar_style_to_string = (
    YAML_PLAIN_SCALAR_STYLE() => ':',
    YAML_SINGLE_QUOTED_SCALAR_STYLE() => "'",
    YAML_DOUBLE_QUOTED_SCALAR_STYLE() => '"',
    YAML_LITERAL_SCALAR_STYLE() => '|',
    YAML_FOLDED_SCALAR_STYLE() => '>',
);


sub event_to_test_suite {
    my ($event, $args) = @_;
    my $ev = $event->{name};
        my $string;
        my $content = $event->{value};

        my $properties = '';
        $properties .= " &$event->{anchor}" if defined $event->{anchor};
        $properties .= " <$event->{tag}>" if defined $event->{tag};

        if ($ev eq 'document_start_event') {
            $string = "+DOC";
            $string .= " ---" unless $event->{implicit};
        }
        elsif ($ev eq 'document_end_event') {
            $string = "-DOC";
            $string .= " ..." unless $event->{implicit};
        }
        elsif ($ev eq 'stream_start_event') {
            $string = "+STR";
        }
        elsif ($ev eq 'stream_end_event') {
            $string = "-STR";
        }
        elsif ($ev eq 'mapping_start_event') {
            $string = "+MAP";
            if ($event->{style} and $event->{style} eq YAML_FLOW_MAPPING_STYLE) {
                $string .= ' {}' if $args->{flow};
            }
            $string .= $properties;
            if (0) {
                # doesn't match yaml-test-suite format
            }
        }
        elsif ($ev eq 'sequence_start_event') {
            $string = "+SEQ";
            if ($event->{style} and $event->{style} eq YAML_FLOW_SEQUENCE_STYLE) {
                $string .= ' []' if $args->{flow};
            }
            $string .= $properties;
            if (0) {
                # doesn't match yaml-test-suite format
            }
        }
        elsif ($ev eq 'mapping_end_event') {
            $string = "-MAP";
        }
        elsif ($ev eq 'sequence_end_event') {
            $string = "-SEQ";
        }
        elsif ($ev eq 'scalar_event') {
            $string = '=VAL';
            $string .= $properties;

            $content =~ s/\\/\\\\/g;
            $content =~ s/\t/\\t/g;
            $content =~ s/\r/\\r/g;
            $content =~ s/\n/\\n/g;
            $content =~ s/[\b]/\\b/g;

            $string .= ' '
                . $scalar_style_to_string{ $event->{style} }
                . $content;
        }
        elsif ($ev eq 'alias_event') {
            $string = "=ALI *$content";
        }
        return $string;
}

sub test_suite_to_event {
    my ($str) = @_;
    my $event = {};
    if ($str =~ s/^\+STR//) {
        $event->{name} = 'stream_start_event';
    }
    elsif ($str =~ s/^\-STR//) {
        $event->{name} = 'stream_end_event';
    }
    elsif ($str =~ s/^\+DOC//) {
        $event->{name} = 'document_start_event';
        if ($str =~ s/^ ---//) {
            $event->{implicit} = 0;
        }
        else {
            $event->{implicit} = 1;
        }
    }
    elsif ($str =~ s/^\-DOC//) {
        $event->{name} = 'document_end_event';
        if ($str =~ s/^ \.\.\.//) {
            $event->{implicit} = 0;
        }
        else {
            $event->{implicit} = 1;
        }
    }
    elsif ($str =~ s/^\+SEQ//) {
        $event->{name} = 'sequence_start_event';
        if ($str =~ s/^ \&(\S+)//) {
            $event->{anchor} = $1;
        }
        if ($str =~ s/^ <(\S+)>//) {
            $event->{tag} = $1;
        }
    }
    elsif ($str =~ s/^\-SEQ//) {
        $event->{name} = 'sequence_end_event';
    }
    elsif ($str =~ s/^\+MAP//) {
        $event->{name} = 'mapping_start_event';
        if ($str =~ s/^ \&(\S+)//) {
            $event->{anchor} = $1;
        }
        if ($str =~ s/^ <(\S+)>//) {
            $event->{tag} = $1;
        }
    }
    elsif ($str =~ s/^\-MAP//) {
        $event->{name} = 'mapping_end_event';
    }
    elsif ($str =~ s/^=VAL//) {
        $event->{name} = 'scalar_event';
        if ($str =~ s/^ <(\S+)>//) {
            $event->{tag} = $1;
        }
        if ($str =~ s/^ [:'">|]//) {
            $event->{style} = $1;
        }
        if ($str =~ s/^(.*)//) {
            $event->{value} = $1;
        }
    }
    elsif ($str =~ s/^=ALI//) {
        $event->{name} = 'alias_event';
        if ($str =~ s/^ \*(.*)//) {
            $event->{value} = $1;
        }
    }
    else {
        die "Could not parse event '$str'";
    }
    return $event;
}


1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Common - Constants and common functions

=head1 SYNOPSIS

    use YAML::PP::Common ':STYLES';
    # or
    use YAML::PP::Common qw/
        YAML_ANY_SCALAR_STYLE YAML_PLAIN_SCALAR_STYLE
        YAML_SINGLE_QUOTED_SCALAR_STYLE YAML_DOUBLE_QUOTED_SCALAR_STYLE
        YAML_LITERAL_SCALAR_STYLE YAML_FOLDED_SCALAR_STYLE
        YAML_QUOTED_SCALAR_STYLE

        YAML_ANY_SEQUENCE_STYLE
        YAML_BLOCK_SEQUENCE_STYLE YAML_FLOW_SEQUENCE_STYLE

        YAML_ANY_MAPPING_STYLE
        YAML_BLOCK_MAPPING_STYLE YAML_FLOW_MAPPING_STYLE
    /;

    use YAML::PP::Common ':PRESERVE';
    # or
    use YAML::PP::Common qw/
        PRESERVE_ALL PRESERVE_ORDER PRESERVE_SCALAR_STYLE PRESERVE_FLOW_STYLE
        PRESERVE_ALIAS
    /:

=head1 DESCRIPTION

This module provides common constants and functions for modules working with
YAML::PP events.

=head1 FUNCTIONS

=over

=item event_to_test_suite

    my $string = YAML::PP::Common::event_to_test_suite($event_prom_parser);

For examples of the returned format look into this distributions's directory
C<yaml-test-suite> which is a copy of
L<https://github.com/yaml/yaml-test-suite>.

=item test_suite_to_event

    my $event = YAML::PP::Common::test_suite_to_event($str);

Turns an event string in test suite format into an event hashref. Not complete
yet.

=back

