#!/usr/bin/env ys-0

use ys::taptest: :all

RC =: "'$DIR/../../sample/rosetta-code'"

fizz-buzz-want =:
  qw(1 2 Fizz 4 Buzz Fizz 7 8 Fizz
  Buzz 11 Fizz 13 14 FizzBuzz 16).join("\n")

test::
- name: ys 100-doors.ys
  cmnd:: "ys $RC/100-doors.ys"
  want: |
    Open doors after 100 passes: 1, 4, 9, 16, 25, 36, 49, 64, 81, 100

- name: ys 100-prisoners.ys
  cmnd:: "ys $RC/100-prisoners.ys 500"
  like: |
    Probability of survival with random search: 0\.0
    Probability of survival with ordered search: 0\.\d+

- name: ys 99-bottles-of-beer.ys
  cmnd:: "ys $RC/99-bottles-of-beer.ys 3"
  want: |
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

- note: "'ys average-loop-length.ys' too slow to test"
# - name: ys average-loop-length.ys
#   cmnd:: "ys $RC/average-loop-length.ys"
#   want: x

- name: ys factorial.ys
  cmnd:: "ys $RC/factorial.ys"
  want: |
    0! -> 1
    1! -> 1
    2! -> 2
    3! -> 6
    4! -> 24
    5! -> 120
    6! -> 720
    7! -> 5040
    8! -> 40320

- name: ys fibonacci-sequence.ys 10
  cmnd:: "ys $RC/fibonacci-sequence.ys 10"
  want: |
    0
    1
    1
    2
    3
    5
    8
    13
    21
    34

- name: ys fizzbuzz.ys 16
  cmnd:: "ys $RC/fizzbuzz.ys 16"
  want:: |
    $fizz-buzz-want

- name: ys function-definition.ys
  cmnd:: "ys $RC/function-definition.ys"
  want: multiply(3, 4, 5) -> 60

- name: ys greatest-common-divisor.ys 42 63
  cmnd:: "ys $RC/greatest-common-divisor.ys 42 63"
  want: gcd(42 63) -> 21

- name: ys hello-world.ys
  cmnd:: "ys $RC/hello-world.ys"
  want: |
    How to run "Hello world!" in YAMLScript (ys):

    1. Install ys (Linux or macOS):

           curl -s https://yamlscript.org/install | bash

       This installs ys into ~/.local/bin by default; make sure that
       directory is in your PATH. Full instructions, including other
       platforms and prefixes, are at
       https://yamlscript.org/doc/install/

    2. Using any plain text editor (nano, vim, Notepad, ...), create
       a file named hello.ys containing these two lines:

           !ys-0
           say: 'Hello world!'

    3. Run it from your terminal:

           ys hello.ys

       The output appears right there in the terminal, like this:

    Hello world!

- name: ys leap-year.ys 2024
  cmnd:: "ys $RC/leap-year.ys 2024"
  want: 2024 is a leap year.

- name: ys palindrome-detection.ys 31337
  cmnd:: "ys $RC/palindrome-detection.ys 31337"
  want: 31337 is not a palindrome.

- name: ys rot-13.ys vendethiel
  cmnd:: "ys $RC/rot-13.ys vendethiel"
  want: iraqrguvry

- name: ys sieve-of-eratosthenes.ys 15
  cmnd:: "ys $RC/sieve-of-eratosthenes.ys 15"
  want: |
    The 6 prime numbers less than 15 are:
    2
    3
    5
    7
    11
    13

- name: ys weird-numbers.ys 5000
  cmnd:: "ys $RC/weird-numbers.ys 5000"
  want: |
    The first 3 weird numbers:
    70 836 4030

done:
