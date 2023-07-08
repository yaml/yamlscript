use YAMLScript::Test;

sub cmd {
    my ($cmd, $want) = @_;
    my ($got) = capture_merged { system $cmd };
    is $got, $want, "Program works: '$cmd'";
}

cmd "$yamlscript $eg/hello-world.ys", "Hello world!
Hello world!
Hello world!
Hello world!
Hello world!
Hello world!
Hello world!
Hello world!
Hello world!
Hello world!
";

cmd "$yamlscript $eg/factorial.ys", "3628800\n";

my $want1 = <<'...';
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

cmd "$yamlscript $eg/99-bottles.ys 3", $want1;
cmd "$eg/99-bottles.ys 3", $want1;

my $want2 = "1
2
Fizz
4
Buzz
Fizz
7
8
Fizz
Buzz
11
Fizz
13
14
FizzBuzz
16
";

cmd "$yamlscript $eg/fizzbuzz.ys 16 1", $want2;
cmd "$yamlscript $eg/fizzbuzz.ys 16 2", $want2;
cmd "$yamlscript $eg/fizzbuzz.ys 16 3", $want2;

my $want3 ="0
1
1
2
3
5
8
13
21
34
";

cmd "$yamlscript $eg/fibonacci-sequence.ys", $want3;
