use strict;
use warnings;
package YAML::PP::Emitter;

our $VERSION = '0.034'; # VERSION
use Data::Dumper;

use YAML::PP::Common qw/
    YAML_PLAIN_SCALAR_STYLE YAML_SINGLE_QUOTED_SCALAR_STYLE
    YAML_DOUBLE_QUOTED_SCALAR_STYLE
    YAML_LITERAL_SCALAR_STYLE YAML_FOLDED_SCALAR_STYLE
    YAML_FLOW_SEQUENCE_STYLE YAML_FLOW_MAPPING_STYLE
/;

use constant DEBUG => $ENV{YAML_PP_EMIT_DEBUG} ? 1 : 0;
use constant DEFAULT_WIDTH => 80;

sub new {
    my ($class, %args) = @_;
    my $self = bless {
        indent => $args{indent} || 2,
        writer => $args{writer},
        width => $args{width} || DEFAULT_WIDTH,
    }, $class;
    $self->init;
    return $self;
}

sub clone {
    my ($self) = @_;
    my $clone = {
        indent => $self->indent,
    };
    return bless $clone, ref $self;
}

sub event_stack { return $_[0]->{event_stack} }
sub set_event_stack { $_[0]->{event_stack} = $_[1] }
sub indent { return $_[0]->{indent} }
sub width { return $_[0]->{width} }
sub line { return $_[0]->{line} }
sub column { return $_[0]->{column} }
sub set_indent { $_[0]->{indent} = $_[1] }
sub writer { $_[0]->{writer} }
sub set_writer { $_[0]->{writer} = $_[1] }
sub tagmap { return $_[0]->{tagmap} }
sub set_tagmap { $_[0]->{tagmap} = $_[1] }

sub init {
    my ($self) = @_;
    unless ($self->writer) {
        $self->set_writer(YAML::PP::Writer->new);
    }
    $self->set_tagmap({
        'tag:yaml.org,2002:' => '!!',
    });
    $self->{open_ended} = 0;
    $self->{line} = 0;
    $self->{column} = 0;
    $self->writer->init;
}

sub mapping_start_event {
    DEBUG and warn __PACKAGE__.':'.__LINE__.": +++ mapping_start_event\n";
    my ($self, $info) = @_;
    my $stack = $self->event_stack;
    my $last = $stack->[-1];
    my $indent = $last->{indent};
    my $new_indent = $indent;
    my $yaml = '';

    my $props = '';
    my $anchor = $info->{anchor};
    my $tag = $info->{tag};
    if (defined $anchor) {
        $anchor = "&$anchor";
    }
    if (defined $tag) {
        $tag = $self->_emit_tag('map', $tag);
    }
    $props = join ' ', grep defined, ($anchor, $tag);

    my $flow = $last->{flow} || 0;
    $flow++ if ($info->{style} || 0) eq YAML_FLOW_MAPPING_STYLE;

    my $newline = 0;
    if ($flow > 1) {
        if ($last->{type} eq 'SEQ') {
            if ($last->{newline}) {
                $yaml .= ' ';
            }
            if ($last->{index} == 0) {
                $yaml .= "[";
            }
            else {
                $yaml .= ",";
            }
        }
        elsif ($last->{type} eq 'MAP') {
            if ($last->{newline}) {
                $yaml .= ' ';
            }
            if ($last->{index} == 0) {
                $yaml .= "{";
            }
            else {
                $yaml .= ",";
            }
        }
        elsif ($last->{type} eq 'MAPVALUE') {
            if ($last->{index} == 0) {
                die "Should not happen (index 0 in MAPVALUE)";
            }
            $yaml .= ": ";
        }
        if ($props) {
            $yaml .= " $props ";
        }
        $new_indent .= ' ' x $self->indent;
    }
    else {
        if ($last->{type} eq 'DOC') {
            $newline = $last->{newline};
        }
        else {
            if ($last->{newline}) {
                $yaml .= "\n";
                $last->{column} = 0;
            }
            if ($last->{type} eq 'MAPVALUE') {
                $new_indent .= ' ' x $self->indent;
                $newline = 1;
            }
            else {
                $new_indent = $indent;
                if (not $props and $self->indent == 1) {
                    $new_indent .= ' ' x 2;
                }
                else {
                    $new_indent .= ' ' x $self->indent;
                }

                if ($last->{column}) {
                    my $space = $self->indent > 1 ? ' ' x ($self->indent - 1) : ' ';
                    $yaml .= $space;
                }
                else {
                    $yaml .= $indent;
                }
                if ($last->{type} eq 'SEQ') {
                    $yaml .= '-';
                }
                elsif ($last->{type} eq 'MAP') {
                    $yaml .= "?";
                    $last->{type} = 'COMPLEX';
                }
                elsif ($last->{type} eq 'COMPLEXVALUE') {
                    $yaml .= ":";
                }
                else {
                    die "Should not happen ($last->{type} in mapping_start)";
                }
                $last->{column} = 1;
            }
            $last->{newline} = 0;
        }
        if ($props) {
            $yaml .= $last->{column} ? ' ' : $indent;
            $yaml .= $props;
            $newline = 1;
        }
    }
    $self->_write($yaml);
    my $new_info = {
        index => 0, indent => $new_indent, info => $info,
        newline => $newline,
        column => $self->column,
        flow => $flow,
    };
    $new_info->{type} = 'MAP';
    push @{ $stack }, $new_info;
    $last->{index}++;
    $self->{open_ended} = 0;
}

sub mapping_end_event {
    DEBUG and warn __PACKAGE__.':'.__LINE__.": +++ mapping_end_event\n";
    my ($self, $info) = @_;
    my $stack = $self->event_stack;

    my $last = pop @{ $stack };
    if ($last->{index} == 0) {
        my $indent = $last->{indent};
        my $zero_indent = $last->{zero_indent};
        if ($last->{zero_indent}) {
            $indent .= ' ' x $self->indent;
        }
        if ($self->column) {
            $self->_write(" {}\n");
        }
        else {
            $self->_write("$indent\{}\n");
        }
    }
    elsif ($last->{flow}) {
        my $yaml = "}";
        if ($last->{flow} == 1) {
            $yaml .= "\n";
        }
        $self->_write("$yaml");
    }
    $last = $stack->[-1];
    $last->{column} = $self->column;
    if ($last->{type} eq 'SEQ') {
    }
    elsif ($last->{type} eq 'MAP') {
        $last->{type} = 'MAPVALUE';
    }
    elsif ($last->{type} eq 'MAPVALUE') {
        $last->{type} = 'MAP';
    }
    elsif ($last->{type} eq 'COMPLEX') {
        $last->{type} = 'COMPLEXVALUE';
    }
    elsif ($last->{type} eq 'COMPLEXVALUE') {
        $last->{type} = 'MAP';
    }
}

sub sequence_start_event {
    DEBUG and warn __PACKAGE__.':'.__LINE__.": +++ sequence_start_event\n";
    my ($self, $info) = @_;
    my $stack = $self->event_stack;
    my $last = $stack->[-1];
    my $indent = $last->{indent};
    my $new_indent = $indent;
    my $yaml = '';

    my $props = '';
    my $anchor = $info->{anchor};
    my $tag = $info->{tag};
    if (defined $anchor) {
        $anchor = "&$anchor";
    }
    if (defined $tag) {
        $tag = $self->_emit_tag('seq', $tag);
    }
    $props = join ' ', grep defined, ($anchor, $tag);

    my $flow = $last->{flow} || 0;
    $flow++ if $flow or ($info->{style} || 0) eq YAML_FLOW_SEQUENCE_STYLE;
    my $newline = 0;
    my $zero_indent = 0;
    if ($flow > 1) {
        if ($last->{type} eq 'SEQ') {
            if ($last->{newline}) {
                $yaml .= ' ';
            }
            if ($last->{index} == 0) {
                $yaml .= "[";
            }
            else {
                $yaml .= ",";
            }
        }
        elsif ($last->{type} eq 'MAP') {
            if ($last->{newline}) {
                $yaml .= ' ';
            }
            if ($last->{index} == 0) {
                $yaml .= "{";
            }
            else {
                $yaml .= ",";
            }
        }
        elsif ($last->{type} eq 'MAPVALUE') {
            if ($last->{index} == 0) {
                die "Should not happen (index 0 in MAPVALUE)";
            }
            $yaml .= ": ";
        }
        if ($props) {
            $yaml .= " $props ";
        }
        $new_indent .= ' ' x $self->indent;
    }
    else {
        if ($last->{type} eq 'DOC') {
            $newline = $last->{newline};
        }
        else {
            if ($last->{newline}) {
                $yaml .= "\n";
                $last->{column} = 0;
            }
            if ($last->{type} eq 'MAPVALUE') {
                $zero_indent = 1;
                $newline = 1;
            }
            else {
                if (not $props and $self->indent == 1) {
                    $new_indent .= ' ' x 2;
                }
                else {
                    $new_indent .= ' ' x $self->indent;
                }
                if ($last->{column}) {
                    my $space = $self->indent > 1 ? ' ' x ($self->indent - 1) : ' ';
                    $yaml .= $space;
                }
                else {
                    $yaml .= $indent;
                }
                if ($last->{type} eq 'SEQ') {
                    $yaml .= "-";
                }
                elsif ($last->{type} eq 'MAP') {
                    $last->{type} = 'COMPLEX';
                    $zero_indent = 1;
                    $yaml .= "?";
                }
                elsif ($last->{type} eq 'COMPLEXVALUE') {
                    $yaml .= ":";
                    $zero_indent = 1;
                }
                else {
                    die "Should not happen ($last->{type} in sequence_start)";
                }
                $last->{column} = 1;
            }
            $last->{newline} = 0;
        }
        if ($props) {
            $yaml .= $last->{column} ? ' ' : $indent;
            $yaml .= $props;
            $newline = 1;
        }
    }
    $self->_write($yaml);
    $last->{index}++;
    my $new_info = {
        index => 0,
        indent => $new_indent,
        info => $info,
        zero_indent => $zero_indent,
        newline => $newline,
        column => $self->column,
        flow => $flow,
    };
    $new_info->{type} = 'SEQ';
    push @{ $stack }, $new_info;
    $self->{open_ended} = 0;
}

sub sequence_end_event {
    DEBUG and warn __PACKAGE__.':'.__LINE__.": +++ sequence_end_event\n";
    my ($self, $info) = @_;
    my $stack = $self->event_stack;

    my $last = pop @{ $stack };
    if ($last->{index} == 0) {
        my $indent = $last->{indent};
        my $zero_indent = $last->{zero_indent};
        if ($last->{zero_indent}) {
            $indent .= ' ' x $self->indent;
        }
        my $yaml .= $self->column ? ' ' : $indent;
        $yaml .= "[]";
        if ($last->{flow} < 2) {
            $yaml .= "\n";
        }
        $self->_write($yaml);
    }
    elsif ($last->{flow}) {
        my $yaml = "]";
        if ($last->{flow} == 1) {
            $yaml .= "\n";
        }
        $self->_write($yaml);
    }
    $last = $stack->[-1];
    $last->{column} = $self->column;
    if ($last->{type} eq 'SEQ') {
    }
    elsif ($last->{type} eq 'MAP') {
        $last->{type} = 'MAPVALUE';
    }
    elsif ($last->{type} eq 'MAPVALUE') {
        $last->{type} = 'MAP';
    }
    elsif ($last->{type} eq 'COMPLEX') {
        $last->{type} = 'COMPLEXVALUE';
    }
    elsif ($last->{type} eq 'COMPLEXVALUE') {
        $last->{type} = 'MAP';
    }
}

my %forbidden_first = (qw/
    ! 1 & 1 * 1 { 1 } 1 [ 1 ] 1 | 1 > 1 @ 1 ` 1 " 1 ' 1
/, '#' => 1, '%' => 1, ',' => 1, " " => 1);
my %forbidden_first_plus_space = (qw/
    ? 1 - 1 : 1
/);

my %control = (
    "\x00" => '\0',
    "\x01" => '\x01',
    "\x02" => '\x02',
    "\x03" => '\x03',
    "\x04" => '\x04',
    "\x05" => '\x05',
    "\x06" => '\x06',
    "\x07" => '\a',
    "\x08" => '\b',
    "\x0b" => '\v',
    "\x0c" => '\f',
    "\x0e" => '\x0e',
    "\x0f" => '\x0f',
    "\x10" => '\x10',
    "\x11" => '\x11',
    "\x12" => '\x12',
    "\x13" => '\x13',
    "\x14" => '\x14',
    "\x15" => '\x15',
    "\x16" => '\x16',
    "\x17" => '\x17',
    "\x18" => '\x18',
    "\x19" => '\x19',
    "\x1a" => '\x1a',
    "\x1b" => '\e',
    "\x1c" => '\x1c',
    "\x1d" => '\x1d',
    "\x1e" => '\x1e',
    "\x1f" => '\x1f',
    "\x7f" => '\x7f',
    "\x80" => '\x80',
    "\x81" => '\x81',
    "\x82" => '\x82',
    "\x83" => '\x83',
    "\x84" => '\x84',
    "\x86" => '\x86',
    "\x87" => '\x87',
    "\x88" => '\x88',
    "\x89" => '\x89',
    "\x8a" => '\x8a',
    "\x8b" => '\x8b',
    "\x8c" => '\x8c',
    "\x8d" => '\x8d',
    "\x8e" => '\x8e',
    "\x8f" => '\x8f',
    "\x90" => '\x90',
    "\x91" => '\x91',
    "\x92" => '\x92',
    "\x93" => '\x93',
    "\x94" => '\x94',
    "\x95" => '\x95',
    "\x96" => '\x96',
    "\x97" => '\x97',
    "\x98" => '\x98',
    "\x99" => '\x99',
    "\x9a" => '\x9a',
    "\x9b" => '\x9b',
    "\x9c" => '\x9c',
    "\x9d" => '\x9d',
    "\x9e" => '\x9e',
    "\x9f" => '\x9f',
    "\x{2029}" => '\P',
    "\x{2028}" => '\L',
    "\x85" => '\N',
    "\xa0" => '\_',
);

my $control_re = '\x00-\x08\x0b\x0c\x0e-\x1f\x7f-\x84\x86-\x9f\x{d800}-\x{dfff}\x{fffe}\x{ffff}\x{2028}\x{2029}\x85\xa0';
my %to_escape = (
    "\n" => '\n',
    "\t" => '\t',
    "\r" => '\r',
    '\\' => '\\\\',
    '"' => '\\"',
    %control,
);
my $escape_re = $control_re . '\n\t\r';
my $escape_re_without_lb = $control_re . '\t\r';


sub scalar_event {
    DEBUG and warn __PACKAGE__.':'.__LINE__.": +++ scalar_event\n";
    my ($self, $info) = @_;
    my $stack = $self->event_stack;
    my $last = $stack->[-1];
    my $indent = $last->{indent};
    my $value = $info->{value};
    my $flow = $last->{flow};

    my $props = '';
    my $anchor = $info->{anchor};
    my $tag = $info->{tag};
    if (defined $anchor) {
        $anchor = "&$anchor";
    }
    if (defined $tag) {
        $tag = $self->_emit_tag('scalar', $tag);
    }
    $props = join ' ', grep defined, ($anchor, $tag);

    DEBUG and local $Data::Dumper::Useqq = 1;
    $value = '' unless defined $value;

    my $style = $self->_find_best_scalar_style(
        info => $info,
        value => $value,
    );

    my $open_ended = 0;

    if ($style == YAML_PLAIN_SCALAR_STYLE) {
        $value =~ s/\n/\n\n/g;
    }
    elsif ($style == YAML_SINGLE_QUOTED_SCALAR_STYLE) {
        my $new_indent = $last->{indent} . (' ' x $self->indent);
        $value =~ s/(\n+)/"\n" x (1 + (length $1))/eg;
        my @lines = split m/\n/, $value, -1;
        if (@lines > 1) {
            for my $line (@lines[1 .. $#lines]) {
                $line = $new_indent . $line
                    if length $line;
            }
        }
        $value = join "\n", @lines;
        $value =~ s/'/''/g;
        $value = "'" . $value . "'";
    }
    elsif ($style == YAML_LITERAL_SCALAR_STYLE) {
        DEBUG and warn __PACKAGE__.':'.__LINE__.$".Data::Dumper->Dump([\$value], ['value']);
        my $indicators = '';
        if ($value =~ m/\A\n* +/) {
            $indicators .= $self->indent;
        }
        my $indent = $indent . ' ' x $self->indent;
        if ($value !~ m/\n\z/) {
            $indicators .= '-';
            $value .= "\n";
        }
        elsif ($value =~ m/(\n|\A)\n\z/) {
            $indicators .= '+';
            $open_ended = 1;
        }
        $value =~ s/^(?=.)/$indent/gm;
        $value = "|$indicators\n$value";
    }
    elsif ($style == YAML_FOLDED_SCALAR_STYLE) {
        DEBUG and warn __PACKAGE__.':'.__LINE__.$".Data::Dumper->Dump([\$value], ['value']);
        my @lines = split /\n/, $value, -1;
        DEBUG and warn __PACKAGE__.':'.__LINE__.$".Data::Dumper->Dump([\@lines], ['lines']);
        my $eol = 0;
        my $indicators = '';
        if ($value =~ m/\A\n* +/) {
            $indicators .= $self->indent;
        }
        my $indent = $indent . ' ' x $self->indent;
        if ($lines[-1] eq '') {
            pop @lines;
            $eol = 1;
        }
        else {
            $indicators .= '-';
        }
        $value = ">$indicators\n";
        for my $i (0 .. $#lines) {
            my $line = $lines[ $i ];
            if (length $line) {
                $value .= "$indent$line\n";
            }
            if ($i != $#lines) {
                $value .= "\n";
            }
        }
    }
    else {
        $value =~ s/([$escape_re"\\])/$to_escape{ $1 } || sprintf '\\u%04x', ord($1)/eg;
        $value = '"' . $value . '"';
    }

    DEBUG and warn __PACKAGE__.':'.__LINE__.": (@$stack)\n";
    my $yaml = $self->_emit_scalar(
        indent => $indent,
        props => $props,
        value => $value,
        style => $style,
    );

    $last->{index}++;
    $last->{newline} = 0;
    $self->_write($yaml);
    $last->{column} = $self->column;
    $self->{open_ended} = $open_ended;
}

sub _find_best_scalar_style {
    my ($self, %args) = @_;
    my $info = $args{info};
    my $style = $info->{style};
    my $value = $args{value};
    my $stack = $self->event_stack;
    my $last = $stack->[-1];
    my $flow = $last->{flow};

    my $first = substr($value, 0, 1);
    if ($value eq '') {
        if ($flow and $last->{type} ne 'MAPVALUE' and $last->{type} ne 'MAP') {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
        elsif (not $style) {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
    }
    # no control characters anywhere
    elsif ($value =~ m/[$control_re]/) {
        $style = YAML_DOUBLE_QUOTED_SCALAR_STYLE;
    }
    $style ||= YAML_PLAIN_SCALAR_STYLE;

    if ($style == YAML_SINGLE_QUOTED_SCALAR_STYLE) {
        if ($value =~ m/ \n/ or $value =~ m/\n / or $value =~ m/^\n/ or $value =~ m/\n$/) {
            $style = YAML_DOUBLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($value eq "\n") {
            $style = YAML_DOUBLE_QUOTED_SCALAR_STYLE;
        }
    }
    elsif ($style == YAML_LITERAL_SCALAR_STYLE or $style == YAML_FOLDED_SCALAR_STYLE) {
        if ($value eq '') {
            $style = YAML_DOUBLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($flow) {
            # no block scalars in flow
            if ($value =~ tr/\n//) {
                $style = YAML_DOUBLE_QUOTED_SCALAR_STYLE;
            }
            else {
                $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
            }
        }
    }
    elsif ($style == YAML_PLAIN_SCALAR_STYLE) {
        if (not length $value) {
        }
        elsif ($value =~ m/[$escape_re_without_lb]/) {
            $style = YAML_DOUBLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($value eq "\n") {
            $style = YAML_DOUBLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($value !~ tr/ //c) {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($value !~ tr/ \n//c) {
            $style = YAML_DOUBLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($value =~ tr/\n//) {
            $style = $flow ? YAML_DOUBLE_QUOTED_SCALAR_STYLE : YAML_LITERAL_SCALAR_STYLE;
        }
        elsif ($forbidden_first{ $first }) {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($flow and $value =~ tr/,[]{}//) {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
        elsif (substr($value, 0, 3) =~ m/^(?:---|\.\.\.)/) {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($value =~ m/: /) {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($value =~ m/ #/) {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($value =~ m/[: \t]\z/) {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($value =~ m/[^\x20-\x3A\x3B-\x7E\x85\xA0-\x{D7FF}\x{E000}-\x{FEFE}\x{FF00}-\x{FFFD}\x{10000}-\x{10FFFF}]/) {
            $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
        }
        elsif ($forbidden_first_plus_space{ $first }) {
            if (length ($value) == 1 or substr($value, 1, 1) =~ m/^\s/) {
                $style = YAML_SINGLE_QUOTED_SCALAR_STYLE;
            }
        }
    }
    if ($style == YAML_SINGLE_QUOTED_SCALAR_STYLE and not $info->{style}) {
        if ($value =~ tr/'// and $value !~ tr/"//) {
            $style = YAML_DOUBLE_QUOTED_SCALAR_STYLE;
        }
    }
    return $style;
}

sub _emit_scalar {
    my ($self, %args) = @_;
    my $props = $args{props};
    my $value = $args{value};
    my $style = $args{style};
    my $stack = $self->event_stack;
    my $last = $stack->[-1];
    my $flow = $last->{flow};

    my $yaml = '';
    my $pvalue = $props;
    if ($props and length $value) {
        $pvalue .= " $value";
    }
    elsif (length $value) {
        $pvalue .= $value;
    }
    if ($flow) {
        if ($props and not length $value) {
            $pvalue .= ' ';
        }
        $yaml = $self->_emit_flow_scalar(
            value => $value,
            pvalue => $pvalue,
            style => $args{style},
        );
    }
    else {
        $yaml = $self->_emit_block_scalar(
            props => $props,
            value => $value,
            pvalue => $pvalue,
            indent => $args{indent},
            style => $args{style},
        );
    }
    return $yaml;
}

sub _emit_block_scalar {
    my ($self, %args) = @_;
    my $props = $args{props};
    my $value = $args{value};
    my $pvalue = $args{pvalue};
    my $indent = $args{indent};
    my $style = $args{style};
    my $stack = $self->event_stack;
    my $last = $stack->[-1];

    my $yaml;
    if ($last->{type} eq 'MAP' or $last->{type} eq 'SEQ') {
        if ($last->{index} == 0 and $last->{newline}) {
            $yaml .= "\n";
            $last->{column} = 0;
            $last->{newline} = 0;
        }
    }
    my $space = ' ';
    my $multiline = ($style == YAML_LITERAL_SCALAR_STYLE or $style == YAML_FOLDED_SCALAR_STYLE);
    if ($last->{type} eq 'MAP') {

        if ($last->{column}) {
            my $space = $self->indent > 1 ? ' ' x ($self->indent - 1) : ' ';
            $yaml .= $space;
        }
        else {
            $yaml .= $indent;
        }
        if ($props and not length $value) {
            $pvalue .= ' ';
        }
        $last->{type} = 'MAPVALUE';
        if ($multiline) {
            # oops, a complex key
            $yaml .= "? ";
            $last->{type} = 'COMPLEXVALUE';
        }
        if (not $multiline) {
            $pvalue .= ":";
        }
    }
    else {
        if ($last->{type} eq 'MAPVALUE') {
            $last->{type} = 'MAP';
        }
        elsif ($last->{type} eq 'DOC') {
        }
        else {
            if ($last->{column}) {
                my $space = $self->indent > 1 ? ' ' x ($self->indent - 1) : ' ';
                $yaml .= $space;
            }
            else {
                $yaml .= $indent;
            }
            if ($last->{type} eq 'COMPLEXVALUE') {
                $last->{type} = 'MAP';
                $yaml .= ":";
            }
            elsif ($last->{type} eq 'SEQ') {
                $yaml .= "-";
            }
            else {
                die "Should not happen ($last->{type} in scalar_event)";

            }
            $last->{column} = 1;
        }

        if (length $pvalue) {
            if ($last->{column}) {
                $pvalue = "$space$pvalue";
            }
        }
        if (not $multiline) {
            $pvalue .= "\n";
        }
    }
    $yaml .= $pvalue;
    return $yaml;
}

sub _emit_flow_scalar {
    my ($self, %args) = @_;
    my $value = $args{value};
    my $pvalue = $args{pvalue};
    my $stack = $self->event_stack;
    my $last = $stack->[-1];

    my $yaml;
    if ($last->{type} eq 'SEQ') {
        if ($last->{index} == 0) {
            if ($self->column) {
                $yaml .= ' ';
            }
            $yaml .= "[";
        }
        else {
            $yaml .= ", ";
        }
    }
    elsif ($last->{type} eq 'MAP') {
        if ($last->{index} == 0) {
            if ($self->column) {
                $yaml .= ' ';
            }
            $yaml .= "{";
        }
        else {
            $yaml .= ", ";
        }
        $last->{type} = 'MAPVALUE';
    }
    elsif ($last->{type} eq 'MAPVALUE') {
        if ($last->{index} == 0) {
            die "Should not happen (index 0 in MAPVALUE)";
        }
        $yaml .= ": ";
        $last->{type} = 'MAP';
    }
    if ($self->column + length $pvalue > $self->width) {
        $yaml .= "\n";
        $yaml .= $last->{indent};
        $yaml .= ' ' x $self->indent;
    }
    $yaml .= $pvalue;
    return $yaml;
}

sub alias_event {
    DEBUG and warn __PACKAGE__.':'.__LINE__.": +++ alias_event\n";
    my ($self, $info) = @_;
    my $stack = $self->event_stack;
    my $last = $stack->[-1];
    my $indent = $last->{indent};
    my $flow = $last->{flow};

    my $alias = '*' . $info->{value};

    my $yaml = '';
    if ($last->{type} eq 'MAP' or $last->{type} eq 'SEQ') {
        if ($last->{index} == 0 and $last->{newline}) {
            $yaml .= "\n";
            $last->{column} = 0;
            $last->{newline} = 0;
        }
    }
    $yaml .= $last->{column} ? ' ' : $indent;
    if ($flow) {
        my $space = '';
        if ($last->{type} eq 'SEQ') {
            if ($last->{index} == 0) {
                if ($flow == 1) {
                    $yaml .= ' ';
                }
                $yaml .= "[";
            }
            else {
                $yaml .= ", ";
            }
        }
        elsif ($last->{type} eq 'MAP') {
            if ($last->{index} == 0) {
                if ($flow == 1) {
                    $yaml .= ' ';
                }
                $yaml .= "{";
            }
            else {
                $yaml .= ", ";
            }
            $last->{type} = 'MAPVALUE';
            $space = ' ';
        }
        elsif ($last->{type} eq 'MAPVALUE') {
            if ($last->{index} == 0) {
                die 23;
                if ($flow == 1) {
                    $yaml .= ' ';
                }
                $yaml .= "{";
            }
            else {
                $yaml .= ": ";
            }
            $last->{type} = 'MAP';
        }
        $yaml .= "$alias$space";
    }
    else {
        if ($last->{type} eq 'MAP') {
            $yaml .= "$alias :";
            $last->{type} = 'MAPVALUE';
        }
        else {

            if ($last->{type} eq 'MAPVALUE') {
                $last->{type} = 'MAP';
            }
            elsif ($last->{type} eq 'DOC') {
                # TODO an alias at document level isn't actually valid
            }
            else {
                if ($last->{type} eq 'COMPLEXVALUE') {
                    $last->{type} = 'MAP';
                    $yaml .= ": ";
                }
                elsif ($last->{type} eq 'COMPLEX') {
                    $yaml .= ": ";
                }
                elsif ($last->{type} eq 'SEQ') {
                    $yaml .= "- ";
                }
                else {
                    die "Unexpected";
                }
            }
            $yaml .= "$alias\n";
        }
    }

    $self->_write("$yaml");
    $last->{index}++;
    $last->{column} = $self->column;
    $self->{open_ended} = 0;
}

sub document_start_event {
    DEBUG and warn __PACKAGE__.':'.__LINE__.": +++ document_start_event\n";
    my ($self, $info) = @_;
    my $newline = 0;
    my $implicit = $info->{implicit};
    if ($info->{version_directive}) {
        if ($self->{open_ended}) {
            $self->_write("...\n");
        }
        $self->_write("%YAML $info->{version_directive}->{major}.$info->{version_directive}->{minor}\n");
        $self->{open_ended} = 0;
        $implicit = 0; # we need ---
    }
    unless ($implicit) {
        $newline = 1;
        $self->_write("---");
    }
    $self->set_event_stack([
        {
        type => 'DOC', index => 0, indent => '', info => $info,
        newline => $newline, column => $self->column,
        }
    ]);
}

sub document_end_event {
    DEBUG and warn __PACKAGE__.':'.__LINE__.": +++ document_end_event\n";
    my ($self, $info) = @_;
    $self->set_event_stack([]);
    if ($self->{open_ended} or not $info->{implicit}) {
        $self->_write("...\n");
        $self->{open_ended} = 0;
    }
    else {
        $self->{open_ended} = 1;
    }
}

sub stream_start_event {
}

sub stream_end_event {
}

sub _emit_tag {
    my ($self, $type, $tag) = @_;
    my $map = $self->tagmap;
    for my $key (sort keys %$map) {
        if ($tag =~ m/^\Q$key\E(.*)/) {
            $tag = $map->{ $key } . $1;
            return $tag;
        }
    }
    if ($tag =~ m/^(!.*)/) {
        $tag = "$1";
    }
    else {
        $tag = "!<$tag>";
    }
    return $tag;
}

sub finish {
    my ($self) = @_;
    $self->writer->finish;
}

sub _write {
    my ($self, $yaml) = @_;
    return unless length $yaml;
    my @lines = split m/\n/, $yaml, -1;
    my $newlines = @lines - 1;
    $self->{line} += $newlines;
    if (length $lines[-1]) {
        if ($newlines) {
            $self->{column} = length $lines[-1];
        }
        else {
            $self->{column} += length $lines[-1];
        }
    }
    else {
        $self->{column} = 0;
    }
    $self->writer->write($yaml);
}

1;

__END__

=pod

=encoding utf-8

=head1 NAME

YAML::PP::Emitter - Emitting events

=head1 SYNOPSIS

    my $emitter = YAML::PP::Emitter->new(
        indent => 4,
    );

    $emitter->init;

    $emitter->stream_start_event;
    $emitter->document_start_event({ implicit => 1 });
    $emitter->sequence_start_event;
    $emitter->scalar_event({ value => $input, style => $style });
    $emitter->sequence_end_event;
    $emitter->document_end_event({ implicit => 1 });
    $emitter->stream_end_event;

    my $yaml = $emitter->writer->output;
    $emitter->finish;

=head1 DESCRIPTION

The emitter emits events to YAML. It provides methods for each event
type. The arguments are mostly the same as the events from L<YAML::PP::Parser>.

=head1 METHODS

=over

=item new

    my $emitter = YAML::PP::Emitter->new(
        indent => 4,
    );

Constructor. Currently takes these options:

=over

=item indent

=item writer

=back

=item stream_start_event, stream_end_event, document_start_event, document_end_event, sequence_start_event, sequence_end_event, mapping_start_event, mapping_end_event, scalar_event, alias_event

=item indent, set_indent

Getter/setter for number of indentation spaces.

TODO: Currently sequences are always zero-indented.

=item writer, set_writer

Getter/setter for the writer object. By default L<YAML::PP::Writer>.
You can pass your own writer if you want to output the resulting YAML yourself.

=item init

Initialize

=item finish

=back

=cut
