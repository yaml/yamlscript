##
# name:      Mo::Golf
# abstract:  Module for Compacting Mo Modules
# author:    Ingy döt Net <ingy@ingy.net>
# license:   perl
# copyright: 2011
# see:
# - Mo

use strict;
use warnings;
package Mo::Golf;

our $VERSION='0.40';

use PPI;

# This is the mapping of common names to shorter forms that still make some
# sense.
my %short_names = (
    (
        map {($_, substr($_, 0, 1))}
        qw(
            args builder class default exports features
            generator import is_lazy method MoPKG name
            nonlazy_defaults options reftype self
        )
    ),
    build_subs => 'B',
    old_constructor => 'C',
    caller_pkg => 'P',
);

my %short_barewords = ( EAGERINIT => q{':E'}, NONLAZY => q{':N'} );

my %hands_off = map {($_,1)} qw'&import *import';

sub import {
    return unless @_ == 2 and $_[1] eq 'golf';
    binmode STDOUT;
    my $text = do { local $/; <> };
    print STDOUT golf( $text );
};

sub golf {
    my ( $text ) = @_;

    my $tree = PPI::Document->new( \$text );

    my %finder_subs = _finder_subs();

    my @order = qw( comments duplicate_whitespace whitespace trailing_whitespace );

    for my $name ( @order ) {
        my $elements = $tree->find( $finder_subs{$name} );
        die $@ if !defined $elements;
        $_->delete for @{ $elements || [] };
    }

    $tree->find( $finder_subs{$_} )
      for qw( del_superfluous_concat del_last_semicolon_in_block separate_version shorten_var_names shorten_barewords );
    die $@ if $@;

    for my $name ( 'double_semicolon' ) {
        my $elements = $tree->find( $finder_subs{$name} );
        die $@ if !defined $elements;
        $_->delete for @{ $elements || [] };
    }

    return $tree->serialize . "\n";
}

sub tok { "PPI::Token::$_[0]" }

sub _finder_subs {
    return (
        comments => sub { $_[1]->isa( tok 'Comment' ) },

        duplicate_whitespace => sub {
            my ( $top, $current ) = @_;
            return 0 if !$current->isa( tok 'Whitespace' );

            $current->set_content(' ') if 1 < length $current->content;

            return 0 if !$current->next_token;
            return 0 if !$current->next_token->isa( tok 'Whitespace' );
            return 1;
        },

        whitespace => sub {
            my ( $top, $current ) = @_;
            return 0 if !$current->isa( tok 'Whitespace' );
            my $prev = $current->previous_token;
            my $next = $current->next_token;

            return 1 if $prev->isa( tok 'Number' ) and $next->isa( tok 'Operator' ) and $next->content =~ /^\W/; # my $P
            return 1 if $prev->isa( tok 'Word' )   and $next->isa( tok 'Operator' ) and $next->content =~ /^\W/; # my $P
            return 1 if $prev->isa( tok 'Symbol' ) and $next->isa( tok 'Operator' ) and $next->content =~ /^\W/; # $VERSION =  but not $v and

            return 1 if $prev->isa( tok 'Operator' ) and $next->isa( tok 'Quote::Single' ) and $next->content =~ /^\W/; # eq ''
            return 1 if $prev->isa( tok 'Operator' ) and $next->isa( tok 'Quote::Double' ) and $next->content =~ /^\W/; # eq ""
            return 1 if $prev->isa( tok 'Operator' ) and $next->isa( tok 'Symbol' )        and $next->content =~ /^\W/; # eq $v
            return 1 if $prev->isa( tok 'Operator' ) and $next->isa( tok 'Structure' )     and $next->content =~ /^\W/; # eq (

            return 1 if $prev->isa( tok 'Word' )       and $next->isa( tok 'Symbol' );           # my $P
            return 1 if $prev->isa( tok 'Word' )       and $next->isa( tok 'Structure' );        # sub {
            return 1 if $prev->isa( tok 'Word' )       and $next->isa( tok 'Quote::Double' );    # eval "
            return 1 if $prev->isa( tok 'Symbol' )     and $next->isa( tok 'Structure' );        # %a )
            return 1 if $prev->isa( tok 'ArrayIndex' ) and $next->isa( tok 'Operator' );         # $#_ ?
            return 1 if $prev->isa( tok 'Word' )       and $next->isa( tok 'Cast' );             # exists &$_
            return 0;
        },

        trailing_whitespace => sub {
            my ( $top, $current ) = @_;
            return 0 if !$current->isa( tok 'Whitespace' );
            my $prev = $current->previous_token;

            return 1 if $prev->isa( tok 'Structure' );                                           # ;[\n\s]
            return 1 if $prev->isa( tok 'Operator' ) and $prev->content =~ /\W$/;                # = 0.24
            return 1 if $prev->isa( tok 'Quote::Double' );                                       # " .
            return 1 if $prev->isa( tok 'Quote::Single' );                                       # ' }

            return 0;
        },

        double_semicolon => sub {
            my ( $top, $current ) = @_;
            return 0 if !$current->isa( tok 'Structure' );
            return 0 if $current->content ne ';';

            my $prev = $current->previous_token;

            return 0 if !$prev->isa( tok 'Structure' );
            return 0 if $prev->content ne ';';

            return 1;
        },

        del_last_semicolon_in_block => sub {
            my ( $top, $current ) = @_;
            return 0 if !$current->isa( 'PPI::Structure::Block' );

            my $last = $current->last_token;

            return 0 if !$last->isa( tok 'Structure' );
            return 0 if $last->content ne '}';

            my $maybe_semi = $last->previous_token;

            return 0 if !$maybe_semi->isa( tok 'Structure' );
            return 0 if $maybe_semi->content ne ';';

            $maybe_semi->delete;

            return 1;
        },

        del_superfluous_concat => sub {
            my ( $top, $current ) = @_;
            return 0 if !$current->isa( tok 'Operator' );

            my $prev = $current->previous_token;
            my $next = $current->next_token;

            return 0 if $current->content ne '.';
            return 0 if !$prev->isa( tok 'Quote::Double' );
            return 0 if !$next->isa( tok 'Quote::Double' );

            $current->delete;
            $prev->set_content( $prev->{separator} . $prev->string . $next->string . $prev->{separator} );
            $next->delete;

            return 1;
        },

        separate_version => sub {
            my ( $top, $current ) = @_;
            return 0 if !$current->isa( 'PPI::Statement' );

            my $first = $current->first_token;
            return 0 if $first->content ne '$VERSION';

            $current->$_( PPI::Token::Whitespace->new( "\n" ) ) for qw( insert_before insert_after );

            return 1;
        },

        shorten_var_names => sub {
            my ( $top, $current ) = @_;
            return 0 if !$current->isa( tok 'Symbol' );

            my $long_name = $current->canonical;

            return 1 if $hands_off{$long_name};
            (my $name = $long_name) =~ s/^([\$\@\%])// or die $long_name;
            my $sigil = $1;
            die "variable $long_name conflicts with shortened var name"
                if grep {
                    $name eq $_
                } values %short_names;

            my $short_name = $short_names{$name};
            $current->set_content( "$sigil$short_name" ) if $short_name;

            return 1;
        },

        shorten_barewords => sub {
            my ( $top, $current ) = @_;
            return 0 if !$current->isa( tok 'Word' );

            my $name = $current->content;

            die "bareword $name conflicts with shortened bareword"
                if grep {
                    $name eq $_
                } values %short_barewords;

            my $short_name = $short_barewords{$name};
            $current->set_content( $short_name ) if $short_name;

            return 1;
        },
    );
}

=head1 SYNOPSIS

    perl -MMo::Golf=golf < src/Mo/foo.pm > lib/Mo/foo.pm

=head1 DESCRIPTION

This is the module that is responsible for taking Mo code (which is
documented and fairly readable) and reducing it to a single undecipherable
line.
