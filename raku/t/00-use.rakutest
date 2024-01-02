#!/usr/bin/env raku

use Test;
sub from-j($t) { ::("Rakudo::Internals::JSON").from-json($t); }


my @xs = 'lib'.IO;
my %fs;
while @xs {
  for @xs.pop.dir -> $f {
    %fs{$f} = 1 if $f.extension eq 'rakumod';
    @xs.push($f) if $f.d;
  }
}

plan 1 +%fs.keys;

my %meta = from-j('META6.json'.IO.slurp);
for %meta<provides>.keys -> $dn {
  %fs{%meta<provides>{$dn}.IO.relative}--;
  use-ok $dn;
}

ok +%fs.grep(*.value != 0) == 0, 'provides 1:1 files in lib';
