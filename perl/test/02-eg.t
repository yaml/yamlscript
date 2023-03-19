use YAMLScript::Test;

sub cmd {
    my ($cmd, $want) = @_;
    my ($got) = capture_merged { system $cmd };
    is $got, $want, "Program works: '$cmd'";
}

cmd "$yamlscript $eg/99-bottles.ys 3", <<'...';
3 bottles of beer on the wall,
3 bottles of beer.
Take one down, pass it around.
2 bottles of beer on the wall.

2 bottles of beer on the wall,
2 bottles of beer.
Take one down, pass it around.
1 bottle of beer on the wall.

1 bottle of beer on the wall,
1 bottle of beer.
Take one down, pass it around.
No more bottles of beer on the wall.

...
