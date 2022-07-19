use strict; use warnings;
package XXX;
our $VERSION = '0.38';
use base 'Exporter';

our @EXPORT = qw( WWW XXX YYY ZZZ DDD );

our $DumpModule = 'YAML::PP';

if ($ENV{PERL_XXX_DUMPER}) {
    _set_dump_module($ENV{PERL_XXX_DUMPER});
}

sub import {
    my ($package, @args) = @_;
    for (my $i = 0; $i < @args; $i++) {
        my $arg = $args[$i];
        if ($arg eq '-with') {
            die "-with requires another argument"
              unless $i++ < @args;
            _set_dump_module($args[ $i ]);
        }
        else {
            next;
        }
        last;
    }
    if (grep /^-?global$/, @args) {
        *main::WWW = \&WWW;
        *main::XXX = \&XXX;
        *main::YYY = \&YYY;
        *main::ZZZ = \&ZZZ;
        *main::DDD = \&DDD;
        $main::WWW = \&WWW;
        $main::XXX = \&XXX;
        $main::YYY = \&YYY;
        $main::ZZZ = \&ZZZ;
        $main::DDD = \&DDD;
    }
    @_ = ($package);
    goto &Exporter::import;
}

sub _set_dump_module {
    my ($module) = @_;
    $DumpModule = $module;
    die "Don't know how to use XXX -with '$DumpModule'"
        unless $DumpModule =~ /^(
                                   (?:YAML|JSON)(?:::.*)?|
                                   Data::Dumper|
                                   Data::Dump(?:::Color)?
                               )$/x;
}

sub _xxx_dump {
    no strict 'refs';
    no warnings;
    $DumpModule ||= 'YAML::PP';
    my $dump_type =
        (substr($DumpModule, 0, 4) eq 'YAML') ? 'yaml' :
        (substr($DumpModule, 0, 4) eq 'JSON') ? 'json' :
        ($DumpModule eq 'Data::Dumper') ? 'dumper' :
        ($DumpModule eq 'Data::Dump') ? 'dump' :
        ($DumpModule eq 'Data::Dump::Color') ? 'dumpcolor' :
        die 'Invalid dump module in $DumpModule';
    if (not defined ${"$DumpModule\::VERSION"}) {
        eval "require $DumpModule; 1" or die $@;
    }
    if ($DumpModule eq 'YAML::PP') {
        return YAML::PP->new(schema => ['Core', 'Perl'])->dump_string(@_) . "...\n";
    }
    if ($dump_type eq 'yaml') {
        return &{"$DumpModule\::Dump"}(@_) . "...\n";
    }
    if ($dump_type eq 'json') {
        return &{"$DumpModule\::encode_json"}(@_);
    }
    if ($dump_type eq 'dumper') {
        local $Data::Dumper::Sortkeys = 1;
        local $Data::Dumper::Indent = 2;
        return Data::Dumper::Dumper(@_);
    }
    if ($dump_type eq 'dump') {
        return Data::Dump::dump(@_) . "\n";
    }
    if ($dump_type eq 'dumpcolor') {
        return Data::Dump::Color::dump(@_) . "\n";
    }
    die "XXX had an internal error";
}

sub _at_line_number {
    my ($file_path, $line_number);
    my $caller = 0;
    while (++$caller) {
        no strict 'refs';
        my $skipper = (caller($caller))[0] . "::XXX_skip";
        next if defined &$skipper and &$skipper();
        ($file_path, $line_number) = (caller($caller))[1,2];
        last;
    }
    "  at $file_path line $line_number\n";
}

sub WWW {
    my $dump = _xxx_dump(@_) . _at_line_number();
    if (defined &main::diag and
        defined &Test::More::diag and
        \&main::diag eq \&Test::More::diag
    ) {
        main::diag($dump);
    }
    else {
        warn($dump);
    }
    return wantarray ? @_ : $_[0];
}

sub XXX {
    die _xxx_dump(@_) . _at_line_number();
}

sub YYY {
    my $dump = _xxx_dump(@_) . _at_line_number();
    if (defined &main::note and
        defined &Test::More::note and
        \&main::note eq \&Test::More::note
    ) {
        main::note($dump);
    }
    else {
        print($dump);
    }
    return wantarray ? @_ : $_[0];
}

sub ZZZ {
    require Carp;
    Carp::confess(_xxx_dump(@_));
}

sub DDD {
    require Enbugger;
    my $debugger = $ENV{PERL_XXX_DEBUGGER} || 'perl5db';
    Enbugger->load_debugger($debugger);
    @_ = 'Enbugger';
    goto Enbugger->can('stop');
}

1;
