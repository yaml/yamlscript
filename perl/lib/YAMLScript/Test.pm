use strict; use warnings;
package YAMLScript::Test;

use YAMLScript::Main;
use Lingy::Test;

use base 'Exporter';

use Test::More;
use YAML::PP;

use Lingy::Printer;
use Lingy::Common;

use YAMLScript::Lang::RT;
use YAMLScript::Reader;

my $reader = YAMLScript::Reader->new;

$ENV{YAMLSCRIPT_TEST} = 1;

our $yamlscript =
    -f './blib/script/yamlscript' ? './blib/script/yamlscript' :
    -f './bin/yamlscript' ? './bin/yamlscript' :
    die "Can't find 'yamlscript' bin script to test";

our $eg =
    -d 'eg' ? 'eg' :
    -d 'example' ? 'example' :
    die "Can't find eg/example directory";

our @EXPORT = (
    @Lingy::Test::EXPORT,
    qw<
        $yamlscript
        test_eval
        test_ys_to_ly
    >,
);

sub fmt {
    local $_ = shift;
    s/\n/\\n/g;
    s/^\s+//;
    s/\s+$//;
    $_;
}

sub label {
    local $_ = shift;
    my ($got, $want) = @_;
    s/\$GOT/$got/g;
    s/\$WANT/$want/g;
    $_;
}

sub map_yaml_tests {
    my ($func, $yaml) = @_;
    my $data = YAML::PP->new->load_string($yaml);
    if (my $last = $ENV{LAST}) {
        $data = [$data->[0 - $last]];
    } elsif (length(my $only = $ENV{ONLY})) {
        die "Invalid setting ONLY='$only'" unless
            $only =~ /^[1-9]\d*$/;
        my $max = @$data;
        die "You said ONLY='$only', but only $max tests"
            if $only > $max;
        $data = [$data->[$only - 1]];
    }
    for my $test (@$data) {
        $func->(@$test);
    }
}

no warnings 'redefine';
sub test_eval {
    map_yaml_tests sub {
        my ($input, $want, $label) =
            (@_ == 2) ? @_ :
            (@_ == 3) ? ($_[1], $_[2], $_[0]) :
            die;
        $label //= "'${\fmt($input)}' -> '${\fmt($want)}'";

        my $got = eval {
            local $YAMLScript::Reader::read_ys = 1;
            join("\n", RT->rep($input));
        };
        $got = $@ if $@;
        chomp $got;

        $got =~ s/^Error: //;

        $label = label($label. $got, $want);
        if (ref($want) eq 'Regexp') {
            like $got, $want, $label;
        } else {
            is $got, $want, $label;
        }
    }, @_;
}

sub test_ys_to_ly {
    map_yaml_tests sub {
        my ($ys, $ly, $label) =
            (@_ == 2) ? @_ :
            (@_ == 3) ? ($_[1], $_[2], $_[0]) :
            die;

        $ys =~ s/\A\s+//;
        $ly =~ s/\A\s+//;

        $label //= "'${\fmt($ys)}' -> '${\fmt($ly)}'";

        my $ast = $reader->read_ys("$ys\n");
        my $got = Lingy::Printer->pr_str($ast);

        $label = label($label, $got, $ly);
        is $got, $ly, $label;
    }, @_;
}

1;
