# ABSTRACT: YAML::PP Rendering functions
use strict;
use warnings;
package YAML::PP::Render;

our $VERSION = '0.034'; # VERSION

use constant TRACE => $ENV{YAML_PP_TRACE} ? 1 : 0;

sub render_quoted {
    my ($self, $style, $lines) = @_;

    my $quoted = '';
    my $addspace = 0;

    for my $i (0 .. $#$lines) {
        my $line = $lines->[ $i ];
        my $value = $line->{value};
        my $last = $i == $#$lines;
        my $first = $i == 0;
        if ($value eq '') {
            if ($first) {
                $addspace = 1;
            }
            elsif ($last) {
                $quoted .= ' ' if $addspace;
            }
            else {
                $addspace = 0;
                $quoted .= "\n";
            }
            next;
        }

        $quoted .= ' ' if $addspace;
        $addspace = 1;
        if ($style eq '"') {
            if ($line->{orig} =~ m/\\$/) {
                $line->{value} =~ s/\\$//;
                $value =~ s/\\$//;
                $addspace = 0;
            }
        }
        $quoted .= $value;
    }
    return $quoted;
}

sub render_block_scalar {
    my ($self, $block_type, $chomp, $lines) = @_;

    my ($folded, $keep, $trim);
    if ($block_type eq '>') {
        $folded = 1;
    }
    if ($chomp eq '+') {
        $keep = 1;
    }
    elsif ($chomp eq '-') {
        $trim = 1;
    }

    my $string = '';
    if (not $keep) {
        # remove trailing empty lines
        while (@$lines) {
            last if $lines->[-1] ne '';
            pop @$lines;
        }
    }
    if ($folded) {

        my $prev = 'START';
        my $trailing = '';
        if ($keep) {
            while (@$lines and $lines->[-1] eq '') {
                pop @$lines;
                $trailing .= "\n";
            }
        }
        for my $i (0 .. $#$lines) {
            my $line = $lines->[ $i ];

            my $type = $line eq ''
                ? 'EMPTY'
                : $line =~ m/\A[ \t]/
                    ? 'MORE'
                    : 'CONTENT';

            if ($prev eq 'MORE' and $type eq 'EMPTY') {
                $type = 'MORE';
            }
            elsif ($prev eq 'CONTENT') {
                if ($type ne 'CONTENT') {
                    $string .= "\n";
                }
                elsif ($type eq 'CONTENT') {
                    $string .= ' ';
                }
            }
            elsif ($prev eq 'START' and $type eq 'EMPTY') {
                $string .= "\n";
                $type = 'START';
            }
            elsif ($prev eq 'EMPTY' and $type ne 'CONTENT') {
                $string .= "\n";
            }

            $string .= $line;

            if ($type eq 'MORE' and $i < $#$lines) {
                $string .= "\n";
            }

            $prev = $type;
        }
        if ($keep) {
            $string .= $trailing;
        }
        $string .= "\n" if @$lines and not $trim;
    }
    else {
        for my $i (0 .. $#$lines) {
            $string .= $lines->[ $i ];
            $string .= "\n" if ($i != $#$lines or not $trim);
        }
    }
    TRACE and warn __PACKAGE__.':'.__LINE__.$".Data::Dumper->Dump([\$string], ['string']);
    return $string;
}

sub render_multi_val {
    my ($self, $multi) = @_;
    my $string = '';
    my $start = 1;
    for my $line (@$multi) {
        if (not $start) {
            if ($line eq '') {
                $string .= "\n";
                $start = 1;
            }
            else {
                $string .= " $line";
            }
        }
        else {
            $string .= $line;
            $start = 0;
        }
    }
    return $string;
}


1;
