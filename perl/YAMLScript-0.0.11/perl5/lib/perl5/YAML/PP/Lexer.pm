use strict;
use warnings;
package YAML::PP::Lexer;

our $VERSION = '0.034'; # VERSION

use constant TRACE => $ENV{YAML_PP_TRACE} ? 1 : 0;
use constant DEBUG => ($ENV{YAML_PP_DEBUG} || $ENV{YAML_PP_TRACE}) ? 1 : 0;

use YAML::PP::Grammar qw/ $GRAMMAR /;
use Carp qw/ croak /;

sub new {
    my ($class, %args) = @_;
    my $self = bless {
        reader => $args{reader},
    }, $class;
    $self->init;
    return $self;
}

sub init {
    my ($self) = @_;
    $self->{next_tokens} = [];
    $self->{next_line} = undef;
    $self->{line} = 0;
    $self->{offset} = 0;
    $self->{flowcontext} = 0;
}

sub next_line { return $_[0]->{next_line} }
sub set_next_line { $_[0]->{next_line} = $_[1] }
sub reader { return $_[0]->{reader} }
sub set_reader { $_[0]->{reader} = $_[1] }
sub next_tokens { return $_[0]->{next_tokens} }
sub line { return $_[0]->{line} }
sub set_line { $_[0]->{line} = $_[1] }
sub offset { return $_[0]->{offset} }
sub set_offset { $_[0]->{offset} = $_[1] }
sub inc_line { return $_[0]->{line}++ }
sub context { return $_[0]->{context} }
sub set_context { $_[0]->{context} = $_[1] }
sub flowcontext { return $_[0]->{flowcontext} }
sub set_flowcontext { $_[0]->{flowcontext} = $_[1] }
sub block { return $_[0]->{block} }
sub set_block { $_[0]->{block} = $_[1] }

my $RE_WS = '[\t ]';
my $RE_LB = '[\r\n]';
my $RE_DOC_END = qr/\A(\.\.\.)(?=$RE_WS|$)/m;
my $RE_DOC_START = qr/\A(---)(?=$RE_WS|$)/m;
my $RE_EOL = qr/\A($RE_WS+#.*|$RE_WS+)\z/;
#my $RE_COMMENT_EOL = qr/\A(#.*)?(?:$RE_LB|\z)/;

#ns-word-char    ::= ns-dec-digit | ns-ascii-letter | “-”
my $RE_NS_WORD_CHAR = '[0-9A-Za-z-]';
my $RE_URI_CHAR = '(?:' . '%[0-9a-fA-F]{2}' .'|'.  q{[0-9A-Za-z#;/?:@&=+$,_.!*'\(\)\[\]-]} . ')';
my $RE_NS_TAG_CHAR = '(?:' . '%[0-9a-fA-F]{2}' .'|'.  q{[0-9A-Za-z#;/?:@&=+$_.~*'\(\)-]} . ')';

#  [#x21-#x7E]          /* 8 bit */
# | #x85 | [#xA0-#xD7FF] | [#xE000-#xFFFD] /* 16 bit */
# | [#x10000-#x10FFFF]                     /* 32 bit */

#nb-char ::= c-printable - b-char - c-byte-order-mark
#my $RE_NB_CHAR = '[\x21-\x7E]';
my $RE_ANCHOR_CAR = '[\x21-\x2B\x2D-\x5A\x5C\x5E-\x7A\x7C\x7E\xA0-\xFF\x{100}-\x{10FFFF}]';

my $RE_PLAIN_START = '[\x21\x22\x24-\x39\x3B-\x7E\xA0-\xFF\x{100}-\x{10FFFF}]';
my $RE_PLAIN_END = '[\x21-\x39\x3B-\x7E\x85\xA0-\x{D7FF}\x{E000}-\x{FEFE}\x{FF00}-\x{FFFD}\x{10000}-\x{10FFFF}]';
my $RE_PLAIN_FIRST = '[\x24\x28-\x29\x2B\x2E-\x39\x3B-\x3D\x41-\x5A\x5C\x5E-\x5F\x61-\x7A\x7E\xA0-\xFF\x{100}-\x{10FFFF}]';

my $RE_PLAIN_START_FLOW = '[\x21\x22\x24-\x2B\x2D-\x39\x3B-\x5A\x5C\x5E-\x7A\x7C\x7E\xA0-\xFF\x{100}-\x{10FFFF}]';
my $RE_PLAIN_END_FLOW = '[\x21-\x2B\x2D-\x39\x3B-\x5A\x5C\x5E-\x7A\x7C\x7E\x85\xA0-\x{D7FF}\x{E000}-\x{FEFE}\x{FF00}-\x{FFFD}\x{10000}-\x{10FFFF}]';
my $RE_PLAIN_FIRST_FLOW = '[\x24\x28-\x29\x2B\x2E-\x39\x3B-\x3D\x41-\x5A\x5C\x5E-\x5F\x61-\x7A\x7C\x7E\xA0-\xFF\x{100}-\x{10FFFF}]';
# c-indicators
#! 21
#" 22
## 23
#% 25
#& 26
#' 27
#* 2A
#, 2C FLOW
#- 2D XX
#: 3A XX
#> 3E
#? 3F XX
#@ 40
#[ 5B FLOW
#] 5D FLOW
#` 60
#{ 7B FLOW
#| 7C
#} 7D FLOW


my $RE_PLAIN_WORD = "(?::+$RE_PLAIN_END|$RE_PLAIN_START)(?::+$RE_PLAIN_END|$RE_PLAIN_END)*";
my $RE_PLAIN_FIRST_WORD = "(?:[:?-]+$RE_PLAIN_END|$RE_PLAIN_FIRST)(?::+$RE_PLAIN_END|$RE_PLAIN_END)*";
my $RE_PLAIN_WORDS = "(?:$RE_PLAIN_FIRST_WORD(?:$RE_WS+$RE_PLAIN_WORD)*)";
my $RE_PLAIN_WORDS2 = "(?:$RE_PLAIN_WORD(?:$RE_WS+$RE_PLAIN_WORD)*)";

my $RE_PLAIN_WORD_FLOW = "(?::+$RE_PLAIN_END_FLOW|$RE_PLAIN_START_FLOW)(?::+$RE_PLAIN_END_FLOW|$RE_PLAIN_END_FLOW)*";
my $RE_PLAIN_FIRST_WORD_FLOW = "(?:[:?-]+$RE_PLAIN_END_FLOW|$RE_PLAIN_FIRST_FLOW)(?::+$RE_PLAIN_END_FLOW|$RE_PLAIN_END_FLOW)*";
my $RE_PLAIN_WORDS_FLOW = "(?:$RE_PLAIN_FIRST_WORD_FLOW(?:$RE_WS+$RE_PLAIN_WORD_FLOW)*)";
my $RE_PLAIN_WORDS_FLOW2 = "(?:$RE_PLAIN_WORD_FLOW(?:$RE_WS+$RE_PLAIN_WORD_FLOW)*)";


#c-secondary-tag-handle  ::= “!” “!”
#c-named-tag-handle  ::= “!” ns-word-char+ “!”
#ns-tag-char ::= ns-uri-char - “!” - c-flow-indicator
#ns-global-tag-prefix    ::= ns-tag-char ns-uri-char*
#c-ns-local-tag-prefix   ::= “!” ns-uri-char*
my $RE_TAG = "!(?:$RE_NS_WORD_CHAR*!$RE_NS_TAG_CHAR+|$RE_NS_TAG_CHAR+|<$RE_URI_CHAR+>|)";

#c-ns-anchor-property    ::= “&” ns-anchor-name
#ns-char ::= nb-char - s-white
#ns-anchor-char  ::= ns-char - c-flow-indicator
#ns-anchor-name  ::= ns-anchor-char+

my $RE_SEQSTART = qr/\A(-)(?=$RE_WS|$)/m;
my $RE_COMPLEX = qr/(\?)(?=$RE_WS|$)/m;
my $RE_COMPLEXCOLON = qr/\A(:)(?=$RE_WS|$)/m;
my $RE_ANCHOR = "&$RE_ANCHOR_CAR+";
my $RE_ALIAS = "\\*$RE_ANCHOR_CAR+";


my %REGEXES = (
    ANCHOR => qr{($RE_ANCHOR)},
    TAG => qr{($RE_TAG)},
    ALIAS => qr{($RE_ALIAS)},
    SINGLEQUOTED => qr{(?:''|[^'\r\n]+)*},
);

sub _fetch_next_line {
    my ($self) = @_;
    my $next_line = $self->next_line;
    if (defined $next_line ) {
        return $next_line;
    }

    my $line = $self->reader->readline;
    unless (defined $line) {
        $self->set_next_line(undef);
        return;
    }
    $self->set_block(1);
    $self->inc_line;
    $line =~ m/\A( *)([^\r\n]*)([\r\n]|\z)/ or die "Unexpected";
    $next_line = [ $1,  $2, $3 ];
    $self->set_next_line($next_line);
    # $ESCAPE_CHAR from YAML.pm
    if ($line =~ tr/\x00-\x08\x0b-\x0c\x0e-\x1f//) {
        $self->exception("Control characters are not allowed");
    }

    return $next_line;
}

my %TOKEN_NAMES = (
    '"' => 'DOUBLEQUOTE',
    "'" => 'SINGLEQUOTE',
    '|' => 'LITERAL',
    '>' => 'FOLDED',
    '!' => 'TAG',
    '*' => 'ALIAS',
    '&' => 'ANCHOR',
    ':' => 'COLON',
    '-' => 'DASH',
    '?' => 'QUESTION',
    '[' => 'FLOWSEQ_START',
    ']' => 'FLOWSEQ_END',
    '{' => 'FLOWMAP_START',
    '}' => 'FLOWMAP_END',
    ',' => 'FLOW_COMMA',
    '---' => 'DOC_START',
    '...' => 'DOC_END',
);


sub fetch_next_tokens {
    my ($self) = @_;
    my $next = $self->next_tokens;
    return $next if @$next;

    my $next_line = $self->_fetch_next_line;
    if (not $next_line) {
        return [];
    }

    my $spaces = $next_line->[0];
    my $yaml = \$next_line->[1];
    if (not length $$yaml) {
        $self->_push_tokens([ EOL => join('', @$next_line), $self->line ]);
        $self->set_next_line(undef);
        return $next;
    }
    if (substr($$yaml, 0, 1) eq '#') {
        $self->_push_tokens([ EOL => join('', @$next_line), $self->line ]);
        $self->set_next_line(undef);
        return $next;
    }
    if (not $spaces and substr($$yaml, 0, 1) eq "%") {
        $self->_fetch_next_tokens_directive($yaml, $next_line->[2]);
        $self->set_context(0);
        $self->set_next_line(undef);
        return $next;
    }
    if (not $spaces and $$yaml =~ s/\A(---|\.\.\.)(?=$RE_WS|\z)//) {
        $self->_push_tokens([ $TOKEN_NAMES{ $1 } => $1, $self->line ]);
    }
    elsif ($self->flowcontext and $$yaml =~ m/\A[ \t]+(#.*)?\z/) {
        $self->_push_tokens([ EOL => join('', @$next_line), $self->line ]);
        $self->set_next_line(undef);
        return $next;
    }
    else {
        $self->_push_tokens([ SPACE => $spaces, $self->line ]);
    }

    my $partial = $self->_fetch_next_tokens($next_line);
    unless ($partial) {
        $self->set_next_line(undef);
    }
    return $next;
}

my %ANCHOR_ALIAS_TAG =    ( '&' => 1, '*' => 1, '!' => 1 );
my %BLOCK_SCALAR =        ( '|' => 1, '>' => 1 );
my %COLON_DASH_QUESTION = ( ':' => 1, '-' => 1, '?' => 1 );
my %QUOTED =              ( '"' => 1, "'" => 1 );
my %FLOW =                ( '{' => 1, '[' => 1, '}' => 1, ']' => 1, ',' => 1 );
my %CONTEXT =             ( '"' => 1, "'" => 1, '>' => 1, '|' => 1 );

my $RE_ESCAPES = qr{(?:
    \\([ \\\/_0abefnrtvLNP\t"]) | \\x([0-9a-fA-F]{2})
    | \\u([A-Fa-f0-9]{4}) | \\U([A-Fa-f0-9]{4,8})
)}x;
my %CONTROL = (
    '\\' => '\\', '/' => '/', n => "\n", t => "\t", r => "\r", b => "\b",
    'a' => "\a", 'b' => "\b", 'e' => "\e", 'f' => "\f", 'v' => "\x0b", "\t" => "\t",
    'P' => "\x{2029}", L => "\x{2028}", 'N' => "\x85",
    '0' => "\0", '_' => "\xa0", ' ' => ' ', q/"/ => q/"/,
);

sub _fetch_next_tokens {
    TRACE and warn __PACKAGE__.':'.__LINE__.": _fetch_next_tokens\n";
    my ($self, $next_line) = @_;

    my $yaml = \$next_line->[1];
    my $eol = $next_line->[2];

    my @tokens;

    while (1) {
        unless (length $$yaml) {
            push @tokens, ( EOL => $eol, $self->line );
            $self->_push_tokens(\@tokens);
            return;
        }
        my $first = substr($$yaml, 0, 1);
        my $plain = 0;

        if ($self->context) {
            if ($$yaml =~ s/\A($RE_WS*)://) {
                push @tokens, ( WS => $1, $self->line ) if $1;
                push @tokens, ( COLON => ':', $self->line );
                $self->set_context(0);
                next;
            }
            if ($$yaml =~ s/\A($RE_WS*(?: #.*))\z//) {
                push @tokens, ( EOL => $1 . $eol, $self->line );
                $self->_push_tokens(\@tokens);
                return;
            }
            $self->set_context(0);
        }
        if ($CONTEXT{ $first }) {
            push @tokens, ( CONTEXT => $first, $self->line );
            $self->_push_tokens(\@tokens);
            return 1;
        }
        elsif ($COLON_DASH_QUESTION{ $first }) {
            my $token_name = $TOKEN_NAMES{ $first };
            if ($$yaml =~ s/\A\Q$first\E($RE_WS+|\z)//) {
                my $after = $1;
                if (not $self->flowcontext and not $self->block) {
                    push @tokens, ERROR => $first . $after, $self->line;
                    $self->_push_tokens(\@tokens);
                    $self->exception("Tabs can not be used for indentation");
                }
                if ($after =~ tr/\t//) {
                    $self->set_block(0);
                }
                my $token_name = $TOKEN_NAMES{ $first };
                push @tokens, ( $token_name => $first, $self->line );
                if (not defined $1) {
                    push @tokens, ( EOL => $eol, $self->line );
                    $self->_push_tokens(\@tokens);
                    return;
                }
                my $ws = $1;
                if ($$yaml =~ s/\A(#.*|)\z//) {
                    push @tokens, ( EOL => $ws . $1 . $eol, $self->line );
                    $self->_push_tokens(\@tokens);
                    return;
                }
                push @tokens, ( WS => $ws, $self->line );
                next;
            }
            elsif ($self->flowcontext and $$yaml =~ s/\A:(?=[,\{\}\[\]])//) {
                push @tokens, ( $token_name => $first, $self->line );
                next;
            }
            $plain = 1;
        }
        elsif ($ANCHOR_ALIAS_TAG{ $first }) {
            my $token_name = $TOKEN_NAMES{ $first };
            my $REGEX = $REGEXES{ $token_name };
            if ($$yaml =~ s/\A$REGEX//) {
                push @tokens, ( $token_name => $1, $self->line );
            }
            else {
                push @tokens, ( "Invalid $token_name" => $$yaml, $self->line );
                $self->_push_tokens(\@tokens);
                return;
            }
        }
        elsif ($first eq ' ' or $first eq "\t") {
            if ($$yaml =~ s/\A($RE_WS+)//) {
                my $ws = $1;
                if ($$yaml =~ s/\A((?:#.*)?\z)//) {
                    push @tokens, ( EOL => $ws . $1 . $eol, $self->line );
                    $self->_push_tokens(\@tokens);
                    return;
                }
                push @tokens, ( WS => $ws, $self->line );
            }
        }
        elsif ($FLOW{ $first }) {
            push @tokens, ( $TOKEN_NAMES{ $first } => $first, $self->line );
            substr($$yaml, 0, 1, '');
            my $flowcontext = $self->flowcontext;
            if ($first eq '{' or $first eq '[') {
                $self->set_flowcontext(++$flowcontext);
            }
            elsif ($first eq '}' or $first eq ']') {
                $self->set_flowcontext(--$flowcontext);
            }
        }
        else {
            $plain = 1;
        }

        if ($plain) {
            push @tokens, ( CONTEXT => '', $self->line );
            $self->_push_tokens(\@tokens);
            return 1;
        }

    }

    return;
}

sub fetch_plain {
    my ($self, $indent, $context) = @_;
    my $next_line = $self->next_line;
    my $yaml = \$next_line->[1];
    my $eol = $next_line->[2];
    my $REGEX = $RE_PLAIN_WORDS;
    if ($self->flowcontext) {
        $REGEX = $RE_PLAIN_WORDS_FLOW;
    }

    my @tokens;
    unless ($$yaml =~ s/\A($REGEX)//) {
        $self->_push_tokens(\@tokens);
        $self->exception("Invalid plain scalar");
    }
    my $plain = $1;
    push @tokens, ( PLAIN => $plain, $self->line );

    if ($$yaml =~ s/\A(?:($RE_WS+#.*)|($RE_WS*))\z//) {
        if (defined $1) {
            push @tokens, ( EOL => $1 . $eol, $self->line );
            $self->_push_tokens(\@tokens);
            $self->set_next_line(undef);
            return;
        }
        else {
            push @tokens, ( EOL => $2. $eol, $self->line );
            $self->set_next_line(undef);
        }
    }
    else {
        $self->_push_tokens(\@tokens);
        my $partial = $self->_fetch_next_tokens($next_line);
        if (not $partial) {
            $self->set_next_line(undef);
        }
        return;
    }

    my $RE2 = $RE_PLAIN_WORDS2;
    if ($self->flowcontext) {
        $RE2 = $RE_PLAIN_WORDS_FLOW2;
    }
    my $fetch_next = 0;
    my @lines = ($plain);
    my @next;
    LOOP: while (1) {
        $next_line = $self->_fetch_next_line;
        if (not $next_line) {
            last LOOP;
        }
        my $spaces = $next_line->[0];
        my $yaml = \$next_line->[1];
        my $eol = $next_line->[2];

        if (not length $$yaml) {
            push @tokens, ( EOL => $spaces . $eol, $self->line );
            $self->set_next_line(undef);
            push @lines, '';
            next LOOP;
        }

        if (not $spaces and $$yaml =~ s/\A(---|\.\.\.)(?=$RE_WS|\z)//) {
            push @next, $TOKEN_NAMES{ $1 } => $1, $self->line;
            $fetch_next = 1;
            last LOOP;
        }
        if ((length $spaces) < $indent) {
            last LOOP;
        }

        my $ws = '';
        if ($$yaml =~ s/\A($RE_WS+)//) {
            $ws = $1;
        }
        if (not length $$yaml) {
            push @tokens, ( EOL => $spaces . $ws . $eol, $self->line );
            $self->set_next_line(undef);
            push @lines, '';
            next LOOP;
        }
        if ($$yaml =~ s/\A(#.*)\z//) {
            push @tokens, ( EOL => $spaces . $ws . $1 . $eol, $self->line );
            $self->set_next_line(undef);
            last LOOP;
        }

        if ($$yaml =~ s/\A($RE2)//) {
            push @tokens, INDENT => $spaces, $self->line;
            push @tokens, WS => $ws, $self->line;
            push @tokens, PLAIN => $1, $self->line;
            push @lines, $1;
            my $ws = '';
            if ($$yaml =~ s/\A($RE_WS+)//) {
                $ws = $1;
            }
            if (not length $$yaml) {
                push @tokens, EOL => $ws . $eol, $self->line;
                $self->set_next_line(undef);
                next LOOP;
            }

            if ($$yaml =~ s/\A(#.*)\z//) {
                push @tokens, EOL => $ws . $1 . $eol, $self->line;
                $self->set_next_line(undef);
                last LOOP;
            }
            else {
                push @tokens, WS => $ws, $self->line if $ws;
                $fetch_next = 1;
            }
        }
        else {
            push @tokens, SPACE => $spaces, $self->line;
            push @tokens, WS => $ws, $self->line;
            if ($self->flowcontext) {
                $fetch_next = 1;
            }
            else {
                push @tokens, ERROR => $$yaml, $self->line;
            }
        }

        last LOOP;

    }
    # remove empty lines at the end
    while (@lines > 1 and $lines[-1] eq '') {
        pop @lines;
    }
    if (@lines > 1) {
        my $value = YAML::PP::Render->render_multi_val(\@lines);
        my @eol;
        if ($tokens[-3] eq 'EOL') {
            @eol = splice @tokens, -3;
        }
        $self->push_subtokens( { name => 'PLAIN_MULTI', value => $value }, \@tokens);
        $self->_push_tokens([ @eol, @next ]);
    }
    else {
        $self->_push_tokens([ @tokens, @next ]);
    }
    @tokens = ();
    if ($fetch_next) {
        my $partial = $self->_fetch_next_tokens($next_line);
        if (not $partial) {
            $self->set_next_line(undef);
        }
    }
    return;
}

sub fetch_block {
    my ($self, $indent, $context) = @_;
    my $next_line = $self->next_line;
    my $yaml = \$next_line->[1];
    my $eol = $next_line->[2];

    my @tokens;
    my $token_name = $TOKEN_NAMES{ $context };
    $$yaml =~ s/\A\Q$context\E// or die "Unexpected";
    push @tokens, ( $token_name => $context, $self->line );
    my $current_indent = $indent;
    my $started = 0;
    my $set_indent = 0;
    my $chomp = '';
    if ($$yaml =~ s/\A([1-9])([+-]?)//) {
        push @tokens, ( BLOCK_SCALAR_INDENT => $1, $self->line );
        $set_indent = $1;
        $chomp = $2 if $2;
        push @tokens, ( BLOCK_SCALAR_CHOMP => $2, $self->line ) if $2;
    }
    elsif ($$yaml =~ s/\A([+-])([1-9])?//) {
        push @tokens, ( BLOCK_SCALAR_CHOMP => $1, $self->line );
        $chomp = $1;
        push @tokens, ( BLOCK_SCALAR_INDENT => $2, $self->line ) if $2;
        $set_indent = $2 if $2;
    }
    if ($set_indent) {
        $started = 1;
        $indent-- if $indent > 0;
        $current_indent = $indent + $set_indent;
    }
    if (not length $$yaml) {
        push @tokens, ( EOL => $eol, $self->line );
    }
    elsif ($$yaml =~ s/\A($RE_WS*(?:$RE_WS#.*|))\z//) {
        push @tokens, ( EOL => $1 . $eol, $self->line );
    }
    else {
        $self->_push_tokens(\@tokens);
        $self->exception("Invalid block scalar");
    }

    my @lines;
    while (1) {
        $self->set_next_line(undef);
        $next_line = $self->_fetch_next_line;
        if (not $next_line) {
            last;
        }
        my $spaces = $next_line->[0];
        my $content = $next_line->[1];
        my $eol = $next_line->[2];
        if (not $spaces and $content =~ m/\A(---|\.\.\.)(?=$RE_WS|\z)/) {
            last;
        }
        if ((length $spaces) < $current_indent) {
            if (length $content) {
                if ($content =~ m/\A\t/) {
                    $self->_push_tokens(\@tokens);
                    $self->exception("Invalid block scalar");
                }
                last;
            }
            else {
                push @lines, '';
                push @tokens, ( EOL => $spaces . $eol, $self->line );
                next;
            }
        }
        if ((length $spaces) > $current_indent) {
            if ($started) {
                ($spaces, my $more_spaces) = unpack "a${current_indent}a*", $spaces;
                $content = $more_spaces . $content;
            }
        }
        unless (length $content) {
            push @lines, '';
            push @tokens, ( INDENT => $spaces, $self->line, EOL => $eol, $self->line );
            unless ($started) {
                $current_indent = length $spaces;
            }
            next;
        }
        unless ($started) {
            $started = 1;
            $current_indent = length $spaces;
        }
        push @lines, $content;
        push @tokens, (
            INDENT => $spaces, $self->line,
            BLOCK_SCALAR_CONTENT => $content, $self->line,
            EOL => $eol, $self->line,
        );
    }
    my $value = YAML::PP::Render->render_block_scalar($context, $chomp, \@lines);
    my @eol = splice @tokens, -3;
    $self->push_subtokens( { name => 'BLOCK_SCALAR', value => $value }, \@tokens );
    $self->_push_tokens([ @eol ]);
    return 0;
}

sub fetch_quoted {
    my ($self, $indent, $context) = @_;
    my $next_line = $self->next_line;
    my $yaml = \$next_line->[1];
    my $spaces = $next_line->[0];

    my $token_name = $TOKEN_NAMES{ $context };
    $$yaml =~ s/\A\Q$context// or die "Unexpected";;
    my @tokens = ( $token_name => $context, $self->line );

    my $start = 1;
    my @values;
    while (1) {

        unless ($start) {
            $next_line = $self->_fetch_next_line or do {
                    for (my $i = 0; $i < @tokens; $i+= 3) {
                        my $token = $tokens[ $i + 1 ];
                        if (ref $token) {
                            $tokens[ $i + 1 ] = $token->{orig};
                        }
                    }
                    $self->_push_tokens(\@tokens);
                    $self->exception("Missing closing quote <$context> at EOF");
                };
            $start = 0;
            $spaces = $next_line->[0];
            $yaml = \$next_line->[1];

            if (not length $$yaml) {
                push @tokens, ( EOL => $spaces . $next_line->[2], $self->line );
                $self->set_next_line(undef);
                push @values, { value => '', orig => '' };
                next;
            }
            elsif (not $spaces and $$yaml =~ m/\A(---|\.\.\.)(?=$RE_WS|\z)/) {
                    for (my $i = 0; $i < @tokens; $i+= 3) {
                        my $token = $tokens[ $i + 1 ];
                        if (ref $token) {
                            $tokens[ $i + 1 ] = $token->{orig};
                        }
                    }
                $self->_push_tokens(\@tokens);
                $self->exception("Missing closing quote <$context> or invalid document marker");
            }
            elsif ((length $spaces) < $indent) {
                for (my $i = 0; $i < @tokens; $i+= 3) {
                    my $token = $tokens[ $i + 1 ];
                    if (ref $token) {
                        $tokens[ $i + 1 ] = $token->{orig};
                    }
                }
                $self->_push_tokens(\@tokens);
                $self->exception("Wrong indendation or missing closing quote <$context>");
            }

            if ($$yaml =~ s/\A($RE_WS+)//) {
                $spaces .= $1;
            }
            push @tokens, ( WS => $spaces, $self->line );
        }

        my $v = $self->_read_quoted_tokens($start, $context, $yaml, \@tokens);
        push @values, $v;
        if ($tokens[-3] eq $token_name) {
            if ($start) {
                $self->push_subtokens(
                    { name => 'QUOTED', value => $v->{value} }, \@tokens
                );
            }
            else {
                my $value = YAML::PP::Render->render_quoted($context, \@values);
                $self->push_subtokens(
                    { name => 'QUOTED_MULTILINE', value => $value }, \@tokens
                );
            }
            $self->set_context(1) if $self->flowcontext;
            if (length $$yaml) {
                my $partial = $self->_fetch_next_tokens($next_line);
                if (not $partial) {
                    $self->set_next_line(undef);
                }
                return 0;
            }
            else {
                @tokens = ();
                push @tokens, ( EOL => $next_line->[2], $self->line );
                $self->_push_tokens(\@tokens);
                $self->set_next_line(undef);
                return;
            }
        }
        $tokens[-2] .= $next_line->[2];
        $self->set_next_line(undef);
        $start = 0;
    }
}

sub _read_quoted_tokens {
    my ($self, $start, $first, $yaml, $tokens) = @_;
    my $quoted = '';
    my $decoded = '';
    my $token_name = $TOKEN_NAMES{ $first };
    my $eol = '';
    if ($first eq "'") {
        my $regex = $REGEXES{SINGLEQUOTED};
        if ($$yaml =~ s/\A($regex)//) {
            $quoted .= $1;
            $decoded .= $1;
            $decoded =~ s/''/'/g;
        }
        unless (length $$yaml) {
            if ($quoted =~ s/($RE_WS+)\z//) {
                $eol = $1;
                $decoded =~ s/($eol)\z//;
            }
        }
    }
    else {
        ($quoted, $decoded, $eol) = $self->_read_doublequoted($yaml);
    }
    my $value = { value => $decoded, orig => $quoted };

    if ($$yaml =~ s/\A$first//) {
        if ($start) {
            push @$tokens, ( $token_name . 'D' => $value, $self->line );
        }
        else {
            push @$tokens, ( $token_name . 'D_LINE' => $value, $self->line );
        }
        push @$tokens, ( $token_name => $first, $self->line );
        return $value;
    }
    if (length $$yaml) {
        push @$tokens, ( $token_name . 'D' => $value->{orig}, $self->line );
        $self->_push_tokens($tokens);
        $self->exception("Invalid quoted <$first> string");
    }

    push @$tokens, ( $token_name . 'D_LINE' => $value, $self->line );
    push @$tokens, ( EOL => $eol, $self->line );

    return $value;
}

sub _read_doublequoted {
    my ($self, $yaml) = @_;
    my $quoted = '';
    my $decoded = '';
    my $eol = '';
    while (1) {
        my $last = 1;
        if ($$yaml =~ s/\A([^"\\ \t]+)//) {
            $quoted .= $1;
            $decoded .= $1;
            $last = 0;
        }
        if ($$yaml =~ s/\A($RE_ESCAPES)//) {
            $quoted .= $1;
            my $dec = defined $2 ? $CONTROL{ $2 }
                        : defined $3 ? chr hex $3
                        : defined $4 ? chr hex $4
                        : chr hex $5;
            $decoded .= $dec;
            $last = 0;
        }
        if ($$yaml =~ s/\A([ \t]+)//) {
            my $spaces = $1;
            if (length $$yaml) {
                $quoted .= $spaces;
                $decoded .= $spaces;
                $last = 0;
            }
            else {
                $eol = $spaces;
                last;
            }
        }
        if ($$yaml =~ s/\A(\\)\z//) {
            $quoted .= $1;
            $decoded .= $1;
            last;
        }
        last if $last;
    }
    return ($quoted, $decoded, $eol);
}

sub _fetch_next_tokens_directive {
    my ($self, $yaml, $eol) = @_;
    my @tokens;

    my $trailing_ws = '';
    my $warn = $ENV{YAML_PP_RESERVED_DIRECTIVE} || 'warn';
    if ($$yaml =~ s/\A(\s*%YAML[ \t]+([0-9]+\.[0-9]+))//) {
        my $dir = $1;
        my $version = $2;
        if ($$yaml =~ s/\A($RE_WS+)//) {
            $trailing_ws = $1;
        }
        elsif (length $$yaml) {
            push @tokens, ( 'Invalid directive' => $dir.$$yaml.$eol, $self->line );
            $self->_push_tokens(\@tokens);
            return;
        }
        if ($version !~ m/^1\.[12]$/) {
            if ($warn eq 'warn') {
                warn "Unsupported YAML version '$dir'";
            }
            elsif ($warn eq 'fatal') {
                push @tokens, ( 'Unsupported YAML version' => $dir, $self->line );
                $self->_push_tokens(\@tokens);
                return;
            }
        }
        push @tokens, ( YAML_DIRECTIVE => $dir, $self->line );
    }
    elsif ($$yaml =~ s/\A(\s*%TAG[ \t]+(!$RE_NS_WORD_CHAR*!|!)[ \t]+(tag:\S+|!$RE_URI_CHAR+))($RE_WS*)//) {
        push @tokens, ( TAG_DIRECTIVE => $1, $self->line );
        # TODO
        my $tag_alias = $2;
        my $tag_url = $3;
        $trailing_ws = $4;
    }
    elsif ($$yaml =~ s/\A(\s*\A%(?:\w+).*)//) {
        push @tokens, ( RESERVED_DIRECTIVE => $1, $self->line );
        if ($warn eq 'warn') {
            warn "Found reserved directive '$1'";
        }
        elsif ($warn eq 'fatal') {
            die "Found reserved directive '$1'";
        }
    }
    else {
        push @tokens, ( 'Invalid directive' => $$yaml, $self->line );
        push @tokens, ( EOL => $eol, $self->line );
        $self->_push_tokens(\@tokens);
        return;
    }
    if (not length $$yaml) {
        push @tokens, ( EOL => $eol, $self->line );
    }
    elsif ($trailing_ws and $$yaml =~ s/\A(#.*)?\z//) {
        push @tokens, ( EOL => "$trailing_ws$1$eol", $self->line );
        $self->_push_tokens(\@tokens);
        return;
    }
    elsif ($$yaml =~ s/\A([ \t]+#.*)?\z//) {
        push @tokens, ( EOL => "$1$eol", $self->line );
        $self->_push_tokens(\@tokens);
        return;
    }
    else {
        push @tokens, ( 'Invalid directive' => $trailing_ws.$$yaml, $self->line );
        push @tokens, ( EOL => $eol, $self->line );
    }
    $self->_push_tokens(\@tokens);
    return;
}

sub _push_tokens {
    my ($self, $new_tokens) = @_;
    my $next = $self->next_tokens;
    my $line = $self->line;
    my $column = $self->offset;

    for (my $i = 0; $i < @$new_tokens; $i += 3) {
        my $value = $new_tokens->[ $i + 1 ];
        my $name = $new_tokens->[ $i ];
        my $line = $new_tokens->[ $i + 2 ];
        my $push = {
            name => $name,
            line => $line,
            column => $column,
            value => $value,
        };
        $column += length $value unless $name eq 'CONTEXT';
        push @$next, $push;
        if ($name eq 'EOL') {
            $column = 0;
        }
    }
    $self->set_offset($column);
    return $next;
}

sub push_subtokens {
    my ($self, $token, $subtokens) = @_;
    my $next = $self->next_tokens;
    my $line = $self->line;
    my $column = $self->offset;
    $token->{column} = $column;
    $token->{subtokens} = \my @sub;

    for (my $i = 0; $i < @$subtokens; $i+=3) {
        my $name = $subtokens->[ $i ];
        my $value = $subtokens->[ $i + 1 ];
        my $line = $subtokens->[ $i + 2 ];
        my $push = {
            name => $subtokens->[ $i ],
            line => $line,
            column => $column,
        };
        if (ref $value eq 'HASH') {
            %$push = ( %$push, %$value );
            $column += length $value->{orig};
        }
        else {
            $push->{value} = $value;
            $column += length $value;
        }
        if ($push->{name} eq 'EOL') {
            $column = 0;
        }
        push @sub, $push;
    }
    $token->{line} = $sub[0]->{line};
    push @$next, $token;
    $self->set_offset($column);
    return $next;
}

sub exception {
    my ($self, $msg) = @_;
    my $next = $self->next_tokens;
    $next = [];
    my $line = @$next ? $next->[0]->{line} : $self->line;
    my @caller = caller(0);
    my $yaml = '';
    if (my $nl = $self->next_line) {
        $yaml = join '', @$nl;
        $yaml = $nl->[1];
    }
    my $e = YAML::PP::Exception->new(
        line => $line,
        column => $self->offset + 1,
        msg => $msg,
        next => $next,
        where => $caller[1] . ' line ' . $caller[2],
        yaml => $yaml,
    );
    croak $e;
}

1;
