# ABSTRACT: YAML Parser
use strict;
use warnings;
package YAML::PP::Parser;

our $VERSION = '0.034'; # VERSION

use constant TRACE => $ENV{YAML_PP_TRACE} ? 1 : 0;
use constant DEBUG => ($ENV{YAML_PP_DEBUG} || $ENV{YAML_PP_TRACE}) ? 1 : 0;

use YAML::PP::Common qw/
    YAML_PLAIN_SCALAR_STYLE YAML_SINGLE_QUOTED_SCALAR_STYLE
    YAML_DOUBLE_QUOTED_SCALAR_STYLE
    YAML_LITERAL_SCALAR_STYLE YAML_FOLDED_SCALAR_STYLE
    YAML_FLOW_SEQUENCE_STYLE YAML_FLOW_MAPPING_STYLE
/;
use YAML::PP::Render;
use YAML::PP::Lexer;
use YAML::PP::Grammar qw/ $GRAMMAR /;
use YAML::PP::Exception;
use YAML::PP::Reader;
use Carp qw/ croak /;


sub new {
    my ($class, %args) = @_;
    my $reader = delete $args{reader} || YAML::PP::Reader->new;
    my $default_yaml_version = delete $args{default_yaml_version};
    my $self = bless {
        default_yaml_version => $default_yaml_version || '1.2',
        lexer => YAML::PP::Lexer->new(
            reader => $reader,
        ),
    }, $class;
    my $receiver = delete $args{receiver};
    if ($receiver) {
        $self->set_receiver($receiver);
    }
    return $self;
}

sub clone {
    my ($self) = @_;
    my $clone = {
        default_yaml_version => $self->default_yaml_version,
        lexer => YAML::PP::Lexer->new(),
    };
    return bless $clone, ref $self;
}

sub receiver { return $_[0]->{receiver} }
sub set_receiver {
    my ($self, $receiver) = @_;
    my $callback;
    if (ref $receiver eq 'CODE') {
        $callback = $receiver;
    }
    else {
        $callback = sub {
            my ($self, $event, $info) = @_;
            return $receiver->$event($info);
        };
    }
    $self->{callback} = $callback;
    $self->{receiver} = $receiver;
}
sub reader { return $_[0]->lexer->{reader} }
sub set_reader {
    my ($self, $reader) = @_;
    $self->lexer->set_reader($reader);
}
sub lexer { return $_[0]->{lexer} }
sub callback { return $_[0]->{callback} }
sub set_callback { $_[0]->{callback} = $_[1] }
sub level { return $#{ $_[0]->{offset} } }
sub offset { return $_[0]->{offset} }
sub set_offset { $_[0]->{offset} = $_[1] }
sub events { return $_[0]->{events} }
sub set_events { $_[0]->{events} = $_[1] }
sub new_node { return $_[0]->{new_node} }
sub set_new_node { $_[0]->{new_node} = $_[1] }
sub tagmap { return $_[0]->{tagmap} }
sub set_tagmap { $_[0]->{tagmap} = $_[1] }
sub tokens { return $_[0]->{tokens} }
sub set_tokens { $_[0]->{tokens} = $_[1] }
sub event_stack { return $_[0]->{event_stack} }
sub set_event_stack { $_[0]->{event_stack} = $_[1] }
sub default_yaml_version { return $_[0]->{default_yaml_version} }
sub yaml_version { return $_[0]->{yaml_version} }
sub set_yaml_version { $_[0]->{yaml_version} = $_[1] }
sub yaml_version_directive { return $_[0]->{yaml_version_directive} }
sub set_yaml_version_directive { $_[0]->{yaml_version_directive} = $_[1] }

sub rule { return $_[0]->{rule} }
sub set_rule {
    my ($self, $name) = @_;
    no warnings 'uninitialized';
    DEBUG and $self->info("set_rule($name)");
    $self->{rule} = $name;
}

sub init {
    my ($self) = @_;
    $self->set_offset([]);
    $self->set_events([]);
    $self->set_new_node(0);
    $self->set_tagmap({
        '!!' => "tag:yaml.org,2002:",
    });
    $self->set_tokens([]);
    $self->set_rule(undef);
    $self->set_event_stack([]);
    $self->set_yaml_version($self->default_yaml_version);
    $self->set_yaml_version_directive(undef);
    $self->lexer->init;
}

sub parse_string {
    my ($self, $yaml) = @_;
    $self->set_reader(YAML::PP::Reader->new( input => $yaml ));
    $self->parse();
}

sub parse_file {
    my ($self, $file) = @_;
    $self->set_reader(YAML::PP::Reader::File->new( input => $file ));
    $self->parse();
}

my %nodetypes = (
    MAPVALUE     => 'NODETYPE_COMPLEX',
    MAP          => 'NODETYPE_MAP',
#    IMAP         => 'NODETYPE_SEQ',
    SEQ          => 'NODETYPE_SEQ',
    SEQ0         => 'NODETYPE_SEQ',
    FLOWMAP      => 'NODETYPE_FLOWMAP',
    FLOWMAPVALUE => 'NODETYPE_FLOWMAPVALUE',
    FLOWSEQ      => 'NODETYPE_FLOWSEQ',
    FLOWSEQ_NEXT => 'FLOWSEQ_NEXT',
    DOC          => 'FULLNODE',
    DOC_END      => 'DOCUMENT_END',
    STR          => 'STREAM',
    END_FLOW     => 'END_FLOW',
);

sub parse {
    my ($self) = @_;
    TRACE and warn "=== parse()\n";
    TRACE and $self->debug_yaml;
    $self->init;
    $self->lexer->init;
    eval {
        $self->start_stream;
        $self->set_rule( 'STREAM' );

        $self->parse_tokens();

        $self->end_stream;
    };
    if (my $error = $@) {
        if (ref $error) {
            croak "$error\n ";
        }
        croak $error;
    }

    DEBUG and $self->highlight_yaml;
    TRACE and $self->debug_tokens;
}

sub lex_next_tokens {
    my ($self) = @_;

    DEBUG and $self->info("----------------> lex_next_tokens");
    TRACE and $self->debug_events;

    my $indent = $self->offset->[-1];
    my $event_types = $self->events;
    my $next_tokens = $self->lexer->fetch_next_tokens($indent);
    return unless @$next_tokens;

    my $next = $next_tokens->[0];

    return 1 if ($next->{name} ne 'SPACE');
    my $flow = $event_types->[-1] =~ m/^FLOW/;
    my $space = length $next->{value};
    my $tokens = $self->tokens;

    if (not $space) {
        shift @$next_tokens;
    }
    else {
        push @$tokens, shift @$next_tokens;
    }
    if ($flow) {
        if ($space >= $indent) {
            return 1;
        }
        $self->exception("Bad indendation in " . $self->events->[-1]);
    }
    $next = $next_tokens->[0];
    if ($space > $indent ) {
        return 1 if $indent < 0;
        unless ($self->new_node) {
            $self->exception("Bad indendation in " . $self->events->[-1]);
        }
        return 1;
    }
    if ($self->new_node) {
        if ($space < $indent) {
            $self->scalar_event({ style => YAML_PLAIN_SCALAR_STYLE, value => '' });
            $self->remove_nodes($space);
        }
        else {
            # unindented sequence starts
            my $exp = $self->events->[-1];
            my $seq_start = $next->{name} eq 'DASH';
            if ( $seq_start and ($exp eq 'MAPVALUE' or $exp eq 'MAP')) {
            }
            else {
                $self->scalar_event({ style => YAML_PLAIN_SCALAR_STYLE, value => '' });
            }
        }
    }
    else {
        if ($space < $indent) {
            $self->remove_nodes($space);
        }
    }

    my $exp = $self->events->[-1];

    if ($exp eq 'SEQ0' and $next->{name} ne 'DASH') {
        TRACE and $self->info("In unindented sequence");
        $self->end_sequence;
        $exp = $self->events->[-1];
    }

    if ($self->offset->[-1] != $space) {
        $self->exception("Expected " . $self->events->[-1]);
    }
    return 1;
}

my %next_event = (
    MAP => 'MAPVALUE',
    IMAP => 'IMAPVALUE',
    MAPVALUE => 'MAP',
    IMAPVALUE => 'IMAP',
    SEQ => 'SEQ',
    SEQ0 => 'SEQ0',
    DOC => 'DOC_END',
    STR => 'STR',
    FLOWSEQ => 'FLOWSEQ_NEXT',
    FLOWSEQ_NEXT => 'FLOWSEQ',
    FLOWMAP => 'FLOWMAPVALUE',
    FLOWMAPVALUE => 'FLOWMAP',
);

my %event_to_method = (
    MAP => 'mapping',
    IMAP => 'mapping',
    FLOWMAP => 'mapping',
    SEQ => 'sequence',
    SEQ0 => 'sequence',
    FLOWSEQ => 'sequence',
    DOC => 'document',
    STR => 'stream',
    VAL => 'scalar',
    ALI => 'alias',
    MAPVALUE => 'mapping',
    IMAPVALUE => 'mapping',
);

#sub process_events {
#    my ($self, $res) = @_;
#
#    my $event_stack = $self->event_stack;
#    return unless @$event_stack;
#
#    if (@$event_stack == 1 and $event_stack->[0]->[0] eq 'properties') {
#        return;
#    }
#
#    my $event_types = $self->events;
#    my $properties;
#    my @send_events;
#    for my $event (@$event_stack) {
#        TRACE and warn __PACKAGE__.':'.__LINE__.$".Data::Dumper->Dump([\$event], ['event']);
#        my ($type, $info) = @$event;
#        if ($type eq 'properties') {
#            $properties = $info;
#        }
#        elsif ($type eq 'scalar') {
#            $info->{name} = 'scalar_event';
#            $event_types->[-1] = $next_event{ $event_types->[-1] };
#            push @send_events, $info;
#        }
#        elsif ($type eq 'begin') {
#            my $name = $info->{name};
#            $info->{name} = $event_to_method{ $name } . '_start_event';
#            push @{ $event_types }, $name;
#            push @{ $self->offset }, $info->{offset};
#            push @send_events, $info;
#        }
#        elsif ($type eq 'end') {
#            my $name = $info->{name};
#            $info->{name} = $event_to_method{ $name } . '_end_event';
#            $self->$type($name, $info);
#            push @send_events, $info;
#            if (@$event_types) {
#                $event_types->[-1] = $next_event{ $event_types->[-1] };
#            }
#        }
#        elsif ($type eq 'alias') {
#            if ($properties) {
#                $self->exception("Parse error: Alias not allowed in this context");
#            }
#            $info->{name} = 'alias_event';
#            $event_types->[-1] = $next_event{ $event_types->[-1] };
#            push @send_events, $info;
#        }
#    }
#    @$event_stack = ();
#    for my $info (@send_events) {
#        DEBUG and $self->debug_event( $info );
#        $self->callback->($self, $info->{name}, $info);
#    }
#}

my %fetch_method = (
    '"' => 'fetch_quoted',
    "'" => 'fetch_quoted',
    '|' => 'fetch_block',
    '>' => 'fetch_block',
    ''  => 'fetch_plain',
);

sub parse_tokens {
    my ($self) = @_;
    my $event_types = $self->events;
    my $offsets = $self->offset;
    my $tokens = $self->tokens;
    my $next_tokens = $self->lexer->next_tokens;

    unless ($self->lex_next_tokens) {
        $self->end_document(1);
        return 0;
    }
    unless ($self->new_node) {
        if ($self->level > 0) {
            my $new_rule = $nodetypes{ $event_types->[-1] }
                or die "Did not find '$event_types->[-1]'";
            $self->set_rule( $new_rule );
        }
    }

    my $rule_name = $self->rule;
    DEBUG and $self->info("----------------> parse_tokens($rule_name)");
    my $rule = $GRAMMAR->{ $rule_name }
        or die "Could not find rule $rule_name";

    TRACE and $self->debug_rules($rule);
    TRACE and $self->debug_yaml;
    DEBUG and $self->debug_next_line;

    RULE: while ($rule_name) {
        DEBUG and $self->info("RULE: $rule_name");
        TRACE and $self->debug_tokens($next_tokens);

        unless (@$next_tokens) {
            $self->exception("No more tokens");
        }
        TRACE and warn __PACKAGE__.':'.__LINE__.$".Data::Dumper->Dump([\$next_tokens->[0]], ['next_token']);
        my $got = $next_tokens->[0]->{name};
        if ($got eq 'CONTEXT') {
            my $context = shift @$next_tokens;
            my $indent = $offsets->[-1];
            $indent++ unless $self->lexer->flowcontext;
            my $method = $fetch_method{ $context->{value} };
            my $partial = $self->lexer->$method($indent, $context->{value});
            next RULE;
        }
        my $def = $rule->{ $got };
        if ($def) {
            push @$tokens, shift @$next_tokens;
        }
        elsif ($def = $rule->{DEFAULT}) {
            $got = 'DEFAULT';
        }
        else {
            $self->expected(
                expected => [keys %$rule],
                got => $next_tokens->[0],
            );
        }

        DEBUG and $self->got("---got $got");
        if (my $sub = $def->{match}) {
            DEBUG and $self->info("CALLBACK $sub");
            $self->$sub(@$tokens ? $tokens->[-1] : ());
        }
        my $eol = $got eq 'EOL';
        my $new = $def->{new};
        if ($new) {
            DEBUG and $self->got("NEW: $new");
            $rule_name = $new;
            $self->set_rule($rule_name);
        }
        elsif ($eol) {
        }
        elsif ($def->{return}) {
            $rule_name = $nodetypes{ $event_types->[-1] }
                or die "Unexpected event type $event_types->[-1]";
            $self->set_rule($rule_name);
        }
        else {
            $rule_name .= " - $got"; # for debugging
            $rule = $def;
            next RULE;
        }
        if ($eol) {
            unless ($self->lex_next_tokens) {
                if ($rule_name eq 'DIRECTIVE') {
                    $self->exception("Directive needs document start");
                }
                $self->end_document(1);
                return 0;
            }
            unless ($self->new_node) {
                if ($self->level > 0) {
                    $rule_name = $nodetypes{ $event_types->[-1] }
                        or die "Did not find '$event_types->[-1]'";
                    $self->set_rule( $rule_name );
                }
            }
            $rule_name = $self->rule;
        }
        $rule = $GRAMMAR->{ $rule_name }
            or die "Unexpected rule $rule_name";

    }

    die "Unexpected";
}

sub end_sequence {
    my ($self) = @_;
    my $event_types = $self->events;
    pop @{ $event_types };
    pop @{ $self->offset };
    my $info = { name => 'sequence_end_event' };
    $self->callback->($self, $info->{name} => $info );
    $event_types->[-1] = $next_event{ $event_types->[-1] };
}

sub remove_nodes {
    my ($self, $space) = @_;
    my $offset = $self->offset;
    my $event_types = $self->events;

    my $exp = $event_types->[-1];
    while (@$offset) {
        if ($offset->[ -1 ] <= $space) {
            last;
        }
        if ($exp eq 'MAPVALUE') {
            $self->scalar_event({ style => YAML_PLAIN_SCALAR_STYLE, value => '' });
            $exp = 'MAP';
        }
        my $info = { name => $exp };
        $info->{name} = $event_to_method{ $exp } . '_end_event';
        pop @{ $event_types };
        pop @{ $offset };
        $self->callback->($self, $info->{name} => $info );
        $event_types->[-1] = $next_event{ $event_types->[-1] };
        $exp = $event_types->[-1];
    }
    return $exp;
}

sub start_stream {
    my ($self) = @_;
    push @{ $self->events }, 'STR';
    push @{ $self->offset }, -1;
    $self->callback->($self, 'stream_start_event', {
        name => 'stream_start_event',
    });
}

sub start_document {
    my ($self, $implicit) = @_;
    push @{ $self->events }, 'DOC';
    push @{ $self->offset }, -1;
    my $directive = $self->yaml_version_directive;
    my %directive;
    if ($directive) {
        my ($major, $minor) = split m/\./, $self->yaml_version;
        %directive = ( version_directive => { major => $major, minor => $minor } );
    }
    $self->callback->($self, 'document_start_event', {
        name => 'document_start_event',
        implicit => $implicit,
        %directive,
    });
    $self->set_yaml_version_directive(undef);
    $self->set_rule( 'FULLNODE' );
    $self->set_new_node(1);
}

sub start_sequence {
    my ($self, $offset) = @_;
    my $offsets = $self->offset;
    if ($offsets->[-1] == $offset) {
        push @{ $self->events }, 'SEQ0';
    }
    else {
        push @{ $self->events }, 'SEQ';
    }
    push @{ $offsets }, $offset;
    my $event_stack = $self->event_stack;
    my $info = { name => 'sequence_start_event' };
    if (@$event_stack and $event_stack->[-1]->[0] eq 'properties') {
        my $properties = pop @$event_stack;
        $self->node_properties($properties->[1], $info);
    }
    $self->callback->($self, 'sequence_start_event', $info);
}

sub start_flow_sequence {
    my ($self, $offset) = @_;
    my $offsets = $self->offset;
    my $new_offset = $offsets->[-1];
    my $event_types = $self->events;
    if ($new_offset < 0) {
        $new_offset = 0;
    }
    elsif ($self->new_node) {
        if ($event_types->[-1] !~ m/^FLOW/) {
            $new_offset++;
        }
    }
    push @{ $self->events }, 'FLOWSEQ';
    push @{ $offsets }, $new_offset;

    my $event_stack = $self->event_stack;
    my $info = { style => YAML_FLOW_SEQUENCE_STYLE, name => 'sequence_start_event'  };
    if (@$event_stack and $event_stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($event_stack, $info);
    }
    $self->callback->($self, 'sequence_start_event', $info);
}

sub start_flow_mapping {
    my ($self, $offset, $implicit_flowseq_map) = @_;
    my $offsets = $self->offset;
    my $new_offset = $offsets->[-1];
    my $event_types = $self->events;
    if ($new_offset < 0) {
        $new_offset = 0;
    }
    elsif ($self->new_node) {
        if ($event_types->[-1] !~ m/^FLOW/) {
            $new_offset++;
        }
    }
    push @{ $self->events }, $implicit_flowseq_map ? 'IMAP' : 'FLOWMAP';
    push @{ $offsets }, $new_offset;

    my $event_stack = $self->event_stack;
    my $info = { name => 'mapping_start_event', style => YAML_FLOW_MAPPING_STYLE };
    if (@$event_stack and $event_stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($event_stack, $info);
    }
    $self->callback->($self, 'mapping_start_event', $info);
}

sub end_flow_sequence {
    my ($self) = @_;
    my $event_types = $self->events;
    pop @{ $event_types };
    pop @{ $self->offset };
    my $info = { name => 'sequence_end_event' };
    $self->callback->($self, $info->{name}, $info);
    if ($event_types->[-1] =~ m/^FLOW|^IMAP/) {
        $event_types->[-1] = $next_event{ $event_types->[-1] };
    }
    else {
        push @$event_types, 'END_FLOW';
    }
}

sub end_flow_mapping {
    my ($self) = @_;
    my $event_types = $self->events;
    pop @{ $event_types };
    pop @{ $self->offset };
    my $info = { name => 'mapping_end_event' };
    $self->callback->($self, $info->{name}, $info);
    if ($event_types->[-1] =~ m/^FLOW|^IMAP/) {
        $event_types->[-1] = $next_event{ $event_types->[-1] };
    }
    else {
        push @$event_types, 'END_FLOW';
    }
}

sub cb_end_outer_flow {
    my ($self) = @_;
    my $event_types = $self->events;
    pop @$event_types;
    $event_types->[-1] = $next_event{ $event_types->[-1] };
}

sub start_mapping {
    my ($self, $offset) = @_;
    my $offsets = $self->offset;
    push @{ $self->events }, 'MAP';
    push @{ $offsets }, $offset;
    my $event_stack = $self->event_stack;
    my $info = { name => 'mapping_start_event' };
    if (@$event_stack and $event_stack->[-1]->[0] eq 'properties') {
        my $properties = pop @$event_stack;
        $self->node_properties($properties->[1], $info);
    }
    $self->callback->($self, 'mapping_start_event', $info);
}

sub end_document {
    my ($self, $implicit) = @_;

    my $event_types = $self->events;
    if ($event_types->[-1] =~ m/FLOW/) {
        die "Unexpected end of flow context";
    }
    if ($self->new_node) {
        $self->scalar_event({ style => YAML_PLAIN_SCALAR_STYLE, value => '' });
    }
    $self->remove_nodes(-1);

    if ($event_types->[-1] eq 'STR') {
        return;
    }
    my $last = pop @{ $event_types };
    if ($last ne 'DOC' and $last ne 'DOC_END') {
        $self->exception("Unexpected event type $last");
    }
    pop @{ $self->offset };
    $self->callback->($self, 'document_end_event', {
        name => 'document_end_event',
        implicit => $implicit,
    });
    if ($self->yaml_version eq '1.2') {
        # In YAML 1.2, directives are only for the following
        # document. In YAML 1.1, they are global
        $self->set_tagmap({ '!!' => "tag:yaml.org,2002:" });
    }
    $event_types->[-1] = $next_event{ $event_types->[-1] };
    $self->set_rule('STREAM');
}

sub end_stream {
    my ($self) = @_;
    my $last = pop @{ $self->events };
    $self->exception("Unexpected event type $last") unless $last eq 'STR';
    pop @{ $self->offset };
    $self->callback->($self, 'stream_end_event', {
        name => 'stream_end_event',
    });
}

sub fetch_inline_properties {
    my ($self, $stack, $info) = @_;
    my $properties = $stack->[-1];

    $properties = $properties->[1];
    my $property_offset;
    if ($properties) {
        for my $p (@{ $properties->{inline} }) {
            my $type = $p->{type};
            if (exists $info->{ $type }) {
                $self->exception("A node can only have one $type");
            }
            $info->{ $type } = $p->{value};
            unless (defined $property_offset) {
                $property_offset = $p->{offset};
                $info->{offset} = $p->{offset};
            }
        }
        delete $properties->{inline};
        undef $properties unless $properties->{newline};
    }

    unless ($properties) {
        pop @$stack;
    }
}

sub node_properties {
    my ($self, $properties, $info) = @_;
    if ($properties) {
        for my $p (@{ $properties->{newline} }) {
            my $type = $p->{type};
            if (exists $info->{ $type }) {
                $self->exception("A node can only have one $type");
            }
            $info->{ $type } = $p->{value};
        }
        undef $properties;
    }
}

sub scalar_event {
    my ($self, $info) = @_;
    my $event_types = $self->events;
    my $event_stack = $self->event_stack;
    if (@$event_stack and $event_stack->[-1]->[0] eq 'properties') {
        my $properties = pop @$event_stack;
        $properties = $self->node_properties($properties->[1], $info);
    }

    $info->{name} = 'scalar_event';
    $self->callback->($self, 'scalar_event', $info);
    $self->set_new_node(0);
    $event_types->[-1] = $next_event{ $event_types->[-1] };
}

sub alias_event {
    my ($self, $info) = @_;
    my $event_stack = $self->event_stack;
    if (@$event_stack and $event_stack->[-1]->[0] eq 'properties') {
        $self->exception("Parse error: Alias not allowed in this context");
    }
    my $event_types = $self->events;
    $info->{name} = 'alias_event';
    $self->callback->($self, 'alias_event', $info);
    $self->set_new_node(0);
    $event_types->[-1] = $next_event{ $event_types->[-1] };
}

sub yaml_to_tokens {
    my ($class, $type, $input) = @_;
    my $yp = YAML::PP::Parser->new( receiver => sub {} );
    my @docs = eval {
        $type eq 'string' ? $yp->parse_string($input) : $yp->parse_file($input);
    };
    my $error = $@;

    my $tokens = $yp->tokens;
    if ($error) {
        my $remaining_tokens = $yp->_remaining_tokens;
        push @$tokens, map { +{ %$_, name => 'ERROR' } } @$remaining_tokens;
    }
    return $error, $tokens;
}

sub _remaining_tokens {
    my ($self) = @_;
    my @tokens;
    my $next = $self->lexer->next_tokens;
    push @tokens, @$next;
    my $next_line = $self->lexer->next_line;
    my $remaining = '';
    if ($next_line) {
        if ($self->lexer->offset > 0) {
            $remaining = $next_line->[1] . $next_line->[2];
        }
        else {
            $remaining = join '', @$next_line;
        }
    }
    $remaining .= $self->reader->read;
    $remaining = '' unless defined $remaining;
    push @tokens, { name => "ERROR", value => $remaining };
    return \@tokens;
}

# deprecated
sub event_to_test_suite {
    # uncoverable subroutine
    my ($self, $event) = @_; # uncoverable statement
    if (ref $event eq 'ARRAY') { # uncoverable statement
        return YAML::PP::Common::event_to_test_suite($event->[1]); # uncoverable statement
    }
    return YAML::PP::Common::event_to_test_suite($event); # uncoverable statement
}

sub debug_events {
    # uncoverable subroutine
    my ($self) = @_; # uncoverable statement
    $self->note("EVENTS: (" # uncoverable statement
        . join (' | ', @{ $_[0]->events }) . ')' # uncoverable statement
    );
    $self->debug_offset; # uncoverable statement
}

sub debug_offset {
    # uncoverable subroutine
    my ($self) = @_; # uncoverable statement
    $self->note(
        qq{OFFSET: (}
        # uncoverable statement count:1
        # uncoverable statement count:2
        # uncoverable statement count:3
        . join (' | ', map { defined $_ ? sprintf "%-3d", $_ : '?' } @{ $_[0]->offset })
        # uncoverable statement
        . qq/) level=@{[ $_[0]->level ]}]}/
    );
}

sub debug_yaml {
    # uncoverable subroutine
    my ($self) = @_; # uncoverable statement
    my $line = $self->lexer->line; # uncoverable statement
    $self->note("LINE NUMBER: $line"); # uncoverable statement
    my $next_tokens = $self->lexer->next_tokens; # uncoverable statement
    if (@$next_tokens) { # uncoverable statement
        $self->debug_tokens($next_tokens); # uncoverable statement
    }
}

sub debug_next_line {
    my ($self) = @_;
    my $next_line = $self->lexer->next_line || [];
    my $line = $next_line->[0];
    $line = '' unless defined $line;
    $line =~ s/( +)$/'·' x length $1/e;
    $line =~ s/\t/▸/g;
    $self->note("NEXT LINE: >>$line<<");
}

sub note {
    my ($self, $msg) = @_;
    $self->_colorize_warn(["yellow"], "============ $msg");
}

sub info {
    my ($self, $msg) = @_;
    $self->_colorize_warn(["cyan"], "============ $msg");
}

sub got {
    my ($self, $msg) = @_;
    $self->_colorize_warn(["green"], "============ $msg");
}

sub _colorize_warn {
    # uncoverable subroutine
    my ($self, $colors, $text) = @_; # uncoverable statement
    require Term::ANSIColor; # uncoverable statement
    warn Term::ANSIColor::colored($colors, $text), "\n"; # uncoverable statement
}

sub debug_event {
    # uncoverable subroutine
    my ($self, $event) = @_; # uncoverable statement
    my $str = YAML::PP::Common::event_to_test_suite($event); # uncoverable statement
    require Term::ANSIColor; # uncoverable statement
    warn Term::ANSIColor::colored(["magenta"], "============ $str"), "\n"; # uncoverable statement
}

sub debug_rules {
    # uncoverable subroutine
    my ($self, $rules) = @_; # uncoverable statement
    local $Data::Dumper::Maxdepth = 2; # uncoverable statement
    $self->note("RULES:"); # uncoverable statement
    for my $rule ($rules) { # uncoverable statement
        if (ref $rule eq 'ARRAY') { # uncoverable statement
            my $first = $rule->[0]; # uncoverable statement
            if (ref $first eq 'SCALAR') { # uncoverable statement
                $self->info("-> $$first"); # uncoverable statement
            }
            else { # uncoverable statement
                if (ref $first eq 'ARRAY') { # uncoverable statement
                    $first = $first->[0]; # uncoverable statement
                }
                $self->info("TYPE $first"); # uncoverable statement
            }
        }
        else { # uncoverable statement
            eval { # uncoverable statement
                my @keys = sort keys %$rule; # uncoverable statement
                $self->info("@keys"); # uncoverable statement
            };
        }
    }
}

sub debug_tokens {
    # uncoverable subroutine
    my ($self, $tokens) = @_; # uncoverable statement
    $tokens ||= $self->tokens; # uncoverable statement
    require Term::ANSIColor; # uncoverable statement
    for my $token (@$tokens) { # uncoverable statement
        my $type = Term::ANSIColor::colored(["green"], # uncoverable statement
            sprintf "%-22s L %2d C %2d ", # uncoverable statement
                $token->{name}, $token->{line}, $token->{column} + 1 # uncoverable statement
        );
        local $Data::Dumper::Useqq = 1; # uncoverable statement
        local $Data::Dumper::Terse = 1; # uncoverable statement
        require Data::Dumper; # uncoverable statement
        my $str = Data::Dumper->Dump([$token->{value}], ['str']); # uncoverable statement
        chomp $str; # uncoverable statement
        $str =~ s/(^.|.$)/Term::ANSIColor::colored(['blue'], $1)/ge; # uncoverable statement
        warn "$type$str\n"; # uncoverable statement
    }

}

sub highlight_yaml {
    my ($self) = @_;
    require YAML::PP::Highlight;
    my $tokens = $self->tokens;
    my $highlighted = YAML::PP::Highlight->ansicolored($tokens);
    warn $highlighted;
}

sub exception {
    my ($self, $msg, %args) = @_;
    my $next = $self->lexer->next_tokens;
    my $line = @$next ? $next->[0]->{line} : $self->lexer->line;
    my $offset = @$next ? $next->[0]->{column} : $self->lexer->offset;
    $offset++;
    my $next_line = $self->lexer->next_line;
    my $remaining = '';
    if ($next_line) {
        if ($self->lexer->offset > 0) {
            $remaining = $next_line->[1] . $next_line->[2];
        }
        else {
            $remaining = join '', @$next_line;
        }
    }
    my $caller = $args{caller} || [ caller(0) ];
    my $e = YAML::PP::Exception->new(
        got => $args{got},
        expected => $args{expected},
        line => $line,
        column => $offset,
        msg => $msg,
        next => $next,
        where => $caller->[1] . ' line ' . $caller->[2],
        yaml => $remaining,
    );
    croak $e;
}

sub expected {
    my ($self, %args) = @_;
    my $expected = $args{expected};
    @$expected = sort grep { m/^[A-Z_]+$/ } @$expected;
    my $got = $args{got}->{name};
    my @caller = caller(0);
    $self->exception("Expected (@$expected), but got $got",
        caller => \@caller,
        expected => $expected,
        got => $args{got},
    );
}

sub cb_tag {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    if (! @$stack or $stack->[-1]->[0] ne 'properties') {
        push @$stack, [ properties => {} ];
    }
    my $last = $stack->[-1]->[1];
    my $tag = $self->_read_tag($token->{value}, $self->tagmap);
    $last->{inline} ||= [];
    push @{ $last->{inline} }, {
        type => 'tag',
        value => $tag,
        offset => $token->{column},
    };
}

sub _read_tag {
    my ($self, $tag, $map) = @_;
    if ($tag eq '!') {
        return "!";
    }
    elsif ($tag =~ m/^!<(.*)>/) {
        return $1;
    }
    elsif ($tag =~ m/^(![^!]*!|!)(.+)/) {
        my $alias = $1;
        my $name = $2;
        $name =~ s/%([0-9a-fA-F]{2})/chr hex $1/eg;
        if (exists $map->{ $alias }) {
            $tag = $map->{ $alias }. $name;
        }
        else {
            if ($alias ne '!' and $alias ne '!!') {
                die "Found undefined tag handle '$alias'";
            }
            $tag = "!$name";
        }
    }
    else {
        die "Invalid tag";
    }
    return $tag;
}

sub cb_anchor {
    my ($self, $token) = @_;
    my $anchor = $token->{value};
    $anchor = substr($anchor, 1);
    my $stack = $self->event_stack;
    if (! @$stack or $stack->[-1]->[0] ne 'properties') {
        push @$stack, [ properties => {} ];
    }
    my $last = $stack->[-1]->[1];
    $last->{inline} ||= [];
    push @{ $last->{inline} }, {
        type => 'anchor',
        value => $anchor,
        offset => $token->{column},
    };
}

sub cb_property_eol {
    my ($self, $res) = @_;
    my $stack = $self->event_stack;
    my $last = $stack->[-1]->[1];
    my $inline = delete $last->{inline} or return;
    my $newline = $last->{newline} ||= [];
    push @$newline, @$inline;
}

sub cb_mapkey {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    my $info = {
        style => YAML_PLAIN_SCALAR_STYLE,
        value => $token->{value},
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    push @{ $stack }, [ scalar => $info ];
}

sub cb_send_mapkey {
    my ($self, $res) = @_;
    my $last = pop @{ $self->event_stack };
    $self->scalar_event($last->[1]);
    $self->set_new_node(1);
}

sub cb_send_scalar {
    my ($self, $res) = @_;
    my $last = pop @{ $self->event_stack };
    return unless $last;
    $self->scalar_event($last->[1]);
    my $e = $self->events;
    if ($e->[-1] eq 'IMAP') {
        $self->end_flow_mapping;
    }
}

sub cb_empty_mapkey {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    my $info = {
        style => YAML_PLAIN_SCALAR_STYLE,
        value => '',
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    $self->scalar_event($info);
    $self->set_new_node(1);
}

sub cb_send_flow_alias {
    my ($self, $token) = @_;
    my $alias = substr($token->{value}, 1);
    $self->alias_event({ value => $alias });
}

sub cb_send_alias {
    my ($self, $token) = @_;
    my $alias = substr($token->{value}, 1);
    $self->alias_event({ value => $alias });
}

sub cb_send_alias_from_stack {
    my ($self, $token) = @_;
    my $last = pop @{ $self->event_stack };
    $self->alias_event($last->[1]);
}

sub cb_alias {
    my ($self, $token) = @_;
    my $alias = substr($token->{value}, 1);
    push @{ $self->event_stack }, [ alias => {
        value => $alias,
        offset => $token->{column},
    }];
}

sub cb_question {
    my ($self, $res) = @_;
    $self->set_new_node(1);
}

sub cb_flow_question {
    my ($self, $res) = @_;
    $self->set_new_node(2);
}

sub cb_empty_complexvalue {
    my ($self, $res) = @_;
    $self->scalar_event({ style => YAML_PLAIN_SCALAR_STYLE, value => '' });
}

sub cb_questionstart {
    my ($self, $token) = @_;
    $self->start_mapping($token->{column});
}

sub cb_complexcolon {
    my ($self, $res) = @_;
    $self->set_new_node(1);
}

sub cb_seqstart {
    my ($self, $token) = @_;
    my $column = $token->{column};
    $self->start_sequence($column);
    $self->set_new_node(1);
}

sub cb_seqitem {
    my ($self, $res) = @_;
    $self->set_new_node(1);
}

sub cb_take_quoted {
    my ($self, $token) = @_;
    my $subtokens = $token->{subtokens};
    my $stack = $self->event_stack;
    my $info = {
        style => $subtokens->[0]->{value} eq '"'
            ? YAML_DOUBLE_QUOTED_SCALAR_STYLE
            : YAML_SINGLE_QUOTED_SCALAR_STYLE,
        value => $token->{value},
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    push @{ $stack }, [ scalar => $info ];
}

sub cb_quoted_multiline {
    my ($self, $token) = @_;
    my $subtokens = $token->{subtokens};
    my $stack = $self->event_stack;
    my $info = {
        style => $subtokens->[0]->{value} eq '"'
            ? YAML_DOUBLE_QUOTED_SCALAR_STYLE
            : YAML_SINGLE_QUOTED_SCALAR_STYLE,
        value => $token->{value},
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    push @{ $stack }, [ scalar => $info ];
    $self->cb_send_scalar;
}

sub cb_take_quoted_key {
    my ($self, $token) = @_;
    $self->cb_take_quoted($token);
    $self->cb_send_mapkey;
}

sub cb_send_plain_multi {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    my $info = {
        style => YAML_PLAIN_SCALAR_STYLE,
        value => $token->{value},
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    push @{ $stack }, [ scalar => $info ];
    $self->cb_send_scalar;
}

sub cb_start_plain {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    my $info = {
            style => YAML_PLAIN_SCALAR_STYLE,
            value => $token->{value},
            offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    push @{ $stack }, [ scalar => $info ];
}

sub cb_start_flowseq {
    my ($self, $token) = @_;
    $self->start_flow_sequence($token->{column});
}

sub cb_start_flowmap {
    my ($self, $token) = @_;
    $self->start_flow_mapping($token->{column});
}

sub cb_end_flowseq {
    my ($self, $res) = @_;
    $self->cb_send_scalar;
    $self->end_flow_sequence;
    $self->set_new_node(0);
}

sub cb_flow_comma {
    my ($self) = @_;
    my $event_types = $self->events;
    $self->set_new_node(0);
    if ($event_types->[-1] =~ m/^FLOWSEQ/) {
        $self->cb_send_scalar;
        $event_types->[-1] = $next_event{ $event_types->[-1] };
    }
}

sub cb_flow_colon {
    my ($self) = @_;
    $self->set_new_node(1);
}

sub cb_empty_flow_mapkey {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    my $info = {
        style => YAML_PLAIN_SCALAR_STYLE,
        value => '',
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    $self->scalar_event($info);
}

sub cb_end_flowmap {
    my ($self, $res) = @_;
    $self->end_flow_mapping;
    $self->set_new_node(0);
}

sub cb_end_flowmap_empty {
    my ($self, $res) = @_;
    $self->cb_empty_flowmap_value;
    $self->end_flow_mapping;
    $self->set_new_node(0);
}

sub cb_flowkey_plain {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    my $info = {
        style => YAML_PLAIN_SCALAR_STYLE,
        value => $token->{value},
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    $self->scalar_event($info);
}

sub cb_flowkey_quoted {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    my $subtokens = $token->{subtokens};
    my $info = {
        style => $subtokens->[0]->{value} eq '"'
            ? YAML_DOUBLE_QUOTED_SCALAR_STYLE
            : YAML_SINGLE_QUOTED_SCALAR_STYLE,
        value => $token->{value},
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    $self->scalar_event($info);
}

sub cb_empty_flowmap_key_value {
    my ($self, $token) = @_;
    $self->cb_empty_flow_mapkey($token);
    $self->cb_empty_flowmap_value;
    $self->cb_flow_comma;
}

sub cb_end_empty_flowmap_key_value {
    my ($self, $token) = @_;
    $self->cb_empty_flow_mapkey($token);
    $self->cb_empty_flowmap_value;
    $self->cb_flow_comma;
    $self->cb_end_flowmap;
}

sub cb_empty_flowmap_value {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    my $info = {
        style => YAML_PLAIN_SCALAR_STYLE,
        value => '',
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    $self->scalar_event($info);
}

sub cb_empty_flowseq_comma {
    my ($self, $token) = @_;
    $self->cb_empty_flowmap_value($token);
    $self->cb_flow_comma;
}

sub cb_empty_flowseq_end {
    my ($self, $token) = @_;
    $self->cb_empty_flowmap_value($token);
    $self->cb_end_flowseq;
}

sub cb_insert_map_alias {
    my ($self, $res) = @_;
    my $stack = $self->event_stack;
    my $scalar = pop @$stack;
    my $info = $scalar->[1];
    $self->start_mapping($info->{offset});
    $self->alias_event($info);
    $self->set_new_node(1);
}

sub cb_insert_map {
    my ($self, $res) = @_;
    my $stack = $self->event_stack;
    my $scalar = pop @$stack;
    my $info = $scalar->[1];
    $self->start_mapping($info->{offset});
    $self->scalar_event($info);
    $self->set_new_node(1);
}

sub cb_insert_implicit_flowseq_map {
    my ($self, $res) = @_;
    my $stack = $self->event_stack;
    my $scalar = pop @$stack;
    my $info = $scalar->[1];
    $self->start_flow_mapping($info->{offset}, 1);
    $self->scalar_event($info);
    $self->set_new_node(1);
}

sub cb_insert_empty_implicit_flowseq_map {
    my ($self, $res) = @_;
    my $stack = $self->event_stack;
    my $scalar = pop @$stack;
    my $info = $scalar->[1];
    $self->start_flow_mapping($info->{offset}, 1);
    $self->cb_empty_flowmap_value;
    $self->set_new_node(2);
}

sub cb_insert_empty_map {
    my ($self, $token) = @_;
    my $stack = $self->event_stack;
    my $info = {
        style => YAML_PLAIN_SCALAR_STYLE,
        value => '',
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    $self->start_mapping($info->{offset});
    $self->scalar_event($info);
    $self->set_new_node(1);
}

sub cb_send_block_scalar {
    my ($self, $token) = @_;
    my $type = $token->{subtokens}->[0]->{value};
    my $stack = $self->event_stack;
    my $info = {
        style => $type eq '|'
            ? YAML_LITERAL_SCALAR_STYLE
            : YAML_FOLDED_SCALAR_STYLE,
        value => $token->{value},
        offset => $token->{column},
    };
    if (@$stack and $stack->[-1]->[0] eq 'properties') {
        $self->fetch_inline_properties($stack, $info);
    }
    push @{ $self->event_stack }, [ scalar => $info ];
    $self->cb_send_scalar;
}

sub cb_end_document {
    my ($self, $token) = @_;
    $self->end_document(0);
}

sub cb_end_document_empty {
    my ($self, $token) = @_;
    $self->end_document(0);
}

sub cb_doc_start_implicit {
    my ($self, $token) = @_;
    $self->start_document(1);
}

sub cb_doc_start_explicit {
    my ($self, $token) = @_;
    $self->start_document(0);
}

sub cb_end_doc_start_document {
    my ($self, $token) = @_;
    $self->end_document(1);
    $self->start_document(0);
}

sub cb_tag_directive {
    my ($self, $token) = @_;
    my ($name, $tag_alias, $tag_url) = split ' ', $token->{value};
    $self->tagmap->{ $tag_alias } = $tag_url;
}

sub cb_reserved_directive {
}

sub cb_set_yaml_version_directive {
    my ($self, $token) = @_;
    if ($self->yaml_version_directive) {
        croak "Found duplicate YAML directive";
    }
    my ($version) = $token->{value} =~ m/^%YAML[ \t]+(1\.[12])/;
    $self->set_yaml_version($version || '1.2');
    $self->set_yaml_version_directive(1);
}

1;
