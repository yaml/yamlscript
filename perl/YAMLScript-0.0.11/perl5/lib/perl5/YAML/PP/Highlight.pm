use strict;
use warnings;
package YAML::PP::Highlight;

our $VERSION = '0.034'; # VERSION

our @EXPORT_OK = qw/ Dump /;

use base 'Exporter';
use YAML::PP;
use YAML::PP::Parser;
use Encode;

sub Dump {
    my (@docs) = @_;
    # Dumping objects is safe, so we enable the Perl schema here
    require YAML::PP::Schema::Perl;
    my $yp = YAML::PP->new( schema => [qw/ + Perl /] );
    my $yaml = $yp->dump_string(@docs);

    my ($error, $tokens) = YAML::PP::Parser->yaml_to_tokens(string => $yaml);
    my $highlighted = YAML::PP::Highlight->ansicolored($tokens);
    encode_utf8 $highlighted;
}


my %ansicolors = (
    ANCHOR => [qw/ green /],
    ALIAS => [qw/ bold green /],
    TAG => [qw/ bold blue /],
    INDENT => [qw/ white on_grey3 /],
    COMMENT => [qw/ grey12 /],
    COLON => [qw/ bold magenta /],
    DASH => [qw/ bold magenta /],
    QUESTION => [qw/ bold magenta /],
    YAML_DIRECTIVE => [qw/ cyan /],
    TAG_DIRECTIVE => [qw/ bold cyan /],
    SINGLEQUOTE => [qw/ bold green /],
    SINGLEQUOTED => [qw/ green /],
    SINGLEQUOTED_LINE => [qw/ green /],
    DOUBLEQUOTE => [qw/ bold green /],
    DOUBLEQUOTED => [qw/ green /],
    DOUBLEQUOTED_LINE => [qw/ green /],
    LITERAL => [qw/ bold yellow /],
    FOLDED => [qw/ bold yellow /],
    DOC_START => [qw/ bold /],
    DOC_END => [qw/ bold /],
    BLOCK_SCALAR_CONTENT => [qw/ yellow /],
    TAB => [qw/ on_blue /],
    ERROR => [qw/ bold red /],
    EOL => [qw/ grey12 /],
    TRAILING_SPACE => [qw/ on_grey6 /],
    FLOWSEQ_START => [qw/ bold magenta /],
    FLOWSEQ_END => [qw/ bold magenta /],
    FLOWMAP_START => [qw/ bold magenta /],
    FLOWMAP_END => [qw/ bold magenta /],
    FLOW_COMMA => [qw/ bold magenta /],
    PLAINKEY => [qw/ bright_blue /],
);

sub ansicolored {
    my ($class, $tokens, %args) = @_;
    my $expand_tabs = $args{expand_tabs};
    $expand_tabs = 1 unless defined $expand_tabs;
    require Term::ANSIColor;

    local $Term::ANSIColor::EACHLINE = "\n";
    my $ansi = '';
    my $highlighted = '';

    my @list = $class->transform($tokens);


    for my $token (@list) {
        my $name = $token->{name};
        my $str = $token->{value};

        my $color = $ansicolors{ $name };
        if ($color) {
            $str = Term::ANSIColor::colored($color, $str);
        }
        $highlighted .= $str;
    }

    if ($expand_tabs) {
        # Tabs can't be displayed with ansicolors
        $highlighted =~ s/\t/' ' x 8/eg;
    }
    $ansi .= $highlighted;
    return $ansi;
}

my %htmlcolors = (
    ANCHOR => 'anchor',
    ALIAS => 'alias',
    SINGLEQUOTE => 'singlequote',
    DOUBLEQUOTE => 'doublequote',
    SINGLEQUOTED => 'singlequoted',
    DOUBLEQUOTED => 'doublequoted',
    SINGLEQUOTED_LINE => 'singlequoted',
    DOUBLEQUOTED_LINE => 'doublequoted',
    INDENT => 'indent',
    DASH => 'dash',
    COLON => 'colon',
    QUESTION => 'question',
    YAML_DIRECTIVE => 'yaml_directive',
    TAG_DIRECTIVE => 'tag_directive',
    TAG => 'tag',
    COMMENT => 'comment',
    LITERAL => 'literal',
    FOLDED => 'folded',
    DOC_START => 'doc_start',
    DOC_END => 'doc_end',
    BLOCK_SCALAR_CONTENT => 'block_scalar_content',
    TAB => 'tab',
    ERROR => 'error',
    EOL => 'eol',
    TRAILING_SPACE => 'trailing_space',
    FLOWSEQ_START => 'flowseq_start',
    FLOWSEQ_END => 'flowseq_end',
    FLOWMAP_START => 'flowmap_start',
    FLOWMAP_END => 'flowmap_end',
    FLOW_COMMA => 'flow_comma',
    PLAINKEY => 'plainkey',
    NOEOL => 'noeol',
);
sub htmlcolored {
    require HTML::Entities;
    my ($class, $tokens) = @_;
    my $html = '';
    my @list = $class->transform($tokens);
    for my $token (@list) {
        my $name = $token->{name};
        my $str = $token->{value};
        my $colorclass = $htmlcolors{ $name } || 'default';
        $str = HTML::Entities::encode_entities($str);
        $html .= qq{<span class="$colorclass">$str</span>};
    }
    return $html;
}

sub transform {
    my ($class, $tokens) = @_;
    my @list;
    for my $token (@$tokens) {
        my @values;
        my $value = $token->{value};
        my $subtokens = $token->{subtokens};
        if ($subtokens) {
            @values = @$subtokens;
        }
        else {
            @values = $token;
        }
        for my $token (@values) {
            my $value = defined $token->{orig} ? $token->{orig} : $token->{value};
            if ($token->{name} eq 'EOL' and not length $value) {
                push @list, { name => 'NOEOL', value => '' };
                next;
            }
            push @list, map {
                    $_ =~ tr/\t/\t/
                    ? { name => 'TAB', value => $_ }
                    : { name => $token->{name}, value => $_ }
                } split m/(\t+)/, $value;
        }
    }
    for my $i (0 .. $#list) {
        my $token = $list[ $i ];
        my $name = $token->{name};
        my $str = $token->{value};
        my $trailing_space = 0;
        if ($token->{name} eq 'EOL') {
            if ($str =~ m/ +([\r\n]|\z)/) {
                $token->{name} = "TRAILING_SPACE";
            }
        }
        elsif ($i < $#list) {
            if ($name eq 'PLAIN') {
                for my $n ($i+1 .. $#list) {
                    my $next = $list[ $n ];
                    last if $next->{name} eq 'EOL';
                    next if $next->{name} =~ m/^(WS|SPACE)$/;
                    if ($next->{name} eq 'COLON') {
                        $token->{name} = 'PLAINKEY';
                    }
                }
            }
            my $next = $list[ $i + 1];
            if ($next->{name} eq 'EOL') {
                if ($str =~ m/ \z/ and $name =~ m/^(BLOCK_SCALAR_CONTENT|WS|INDENT)$/) {
                    $token->{name} = "TRAILING_SPACE";
                }
            }
        }
    }
    return @list;
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Highlight - Syntax highlighting utilities

=head1 SYNOPSIS


    use YAML::PP::Highlight qw/ Dump /;

    my $highlighted = Dump $data;

=head1 FUNCTIONS

=over

=item Dump

=back

    use YAML::PP::Highlight qw/ Dump /;

    my $highlighted = Dump $data;
    my $highlighted = Dump @docs;

It will dump the given data, and then parse it again to create tokens, which
are then highlighted with ansi colors.

The return value is ansi colored YAML.
