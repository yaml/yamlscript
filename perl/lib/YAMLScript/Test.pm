use strict; use warnings;
package YAMLScript::Test;

use Lingy::Test;

use base 'Exporter';

use Test::More;

use Lingy::Printer;
use Lingy::Common;

use YAMLScript::Reader;
use YAMLScript::RT;

our $rt = YAMLScript::RT->init;

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
    '$yamlscript',
    'expr',
);

sub collapse {
    local $_ = shift;
    s/\s\s+/ /g;
    s/^ //;
    chomp;
    $_;
}

sub line {
    local $_ = shift;
    s/\n/\\n/g;
    $_;
}

no warnings 'redefine';
sub test {
    my ($input, $want, $label) = @_;
    $label //= "'${\ collapse($input)}' -> '${\line $want}'";

    my $got = eval {
        local $YAMLScript::Reader::read_ys = 1;
        join("\n", $rt->rep($input));
    };
    $got = $@ if $@;
    chomp $got;

    $got =~ s/^Error: //;

    if (ref($want) eq 'Regexp') {
        like $got, $want, $label;
    } else {
        is $got, $want, $label;
    }
}

sub expr {
    my ($ys, $ly, $label) = @_;

    $ys =~ s/\A\s+//;
    $ly =~ s/\A\s+//;

    $label //= "'${\ collapse($ys)}' -> '${\line $ly}'";

    my $ast = $reader->read_ys("$ys\n");
    my $got = Lingy::Printer::pr_str($ast);

    is $got, $ly, $label;
}

1;
