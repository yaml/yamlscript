use strict; use warnings;
package YAMLScript::ReadLine;

use base 'Lingy::ReadLine';

{
    package Lingy::ReadLine;
    use constant RL => YAMLScript::ReadLine->new;
}

my $home = $ENV{HOME};

sub history_file {
    my $history_file = "$ENV{PWD}/.yamlscript_history";
    $history_file = "$home/.yamlscript_history"
        unless -w $history_file;
    return $history_file;
}

sub multi_start {
    my ($self, $line) = @_;
    (
        $line =~ /^---(?=\s|\z)/ and
        not $line =~ /^\.\.\.\z/m
    );
}

sub multi_stop{
    my ($self, $line) = @_;
    $line eq '...';
}

1;
