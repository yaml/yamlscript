#!/usr/bin/env bash

source test/init

RC=$ROOT/sample/rosetta-code


cmd="ys $RC/hello-world.ys"
is "$($cmd)" "\
Hello, world!
Hello, world!
Hello, world!
Hello, world!
Hello, world!
Hello, world!
Hello, world!
Hello, world!" "$cmd"


cmd="ys $RC/100-doors.ys"
is "$($cmd)" "\
Open doors after 100 passes:
1, 4, 9, 16, 25, 36, 49, 64, 81, 100" \
  "$cmd"


cmd="ys $RC/factorial.ys 10"
is "$($cmd)" "10! = 3628800" "$cmd"


cmd="ys $RC/greatest-common-denominator.ys 65 78"
is "$($cmd)" "gcd(65 78) = 13" "$cmd"


cmd="ys $RC/leap-year.ys 2024"
is "$($cmd)" "2024 is a leap year." "$cmd"


cmd="ys $RC/fibonacci.ys 5"
is "$($cmd)" "\
0
1
1
2
3" "$cmd"


cmd="ys $RC/99-bottles-of-beer.ys 3"
is "$($cmd)" "\
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
No more bottles of beer on the wall." \
  "$cmd"


want="\
1
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
16"

cmd="ys $RC/fizzbuzz.ys"
is "$($cmd | tail -n+2 | head -n16)" "$want" "$cmd"

cmd="ys $RC/fizzbuzz.ys 16"
is "$($cmd | tail -n+2)" "$want" "$cmd"

cmd="ys $RC/fizzbuzz.ys 16 1"
is "$($cmd | tail -n+2)" "$want" "$cmd"

cmd="ys $RC/fizzbuzz.ys 16 2"
is "$($cmd | tail -n+2)" "$want" "$cmd"

cmd="ys $RC/fizzbuzz.ys 16 3"
is "$($cmd | tail -n+2)" "$want" "$cmd"


done-testing
