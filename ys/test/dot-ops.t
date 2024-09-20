#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: :all

base =:
  if CWD =~ /\/yamlscript$/:
    then: "$CWD/ys"
    else: CWD

test::
- code: -' '.?
- code: -[nil].?
- code: -{1 2}.?
- code: true.?

# - code: a(42).?~ == 42
# - code: a(0).?~ == nil

- code: -''.!
- code: a(0).!
- code: a(0.0).!
- code: -[].!
- code: -{}.!
- code: nil.!
- code: false.!

- code: -''.??
- code: a(0).??
- code: a(0.0).??
- code: -[].??
- code: -{}.??

- code: nil.!!
- code: false.!!

- code: a(1).0?.!!
- code: a(0).0?
- code: a(0.0).0?
- code: a(1).0!

- code: a(1).1?
- code: a(1.0).1?
- code: (1 + 1).2?
- code: a(2.0).2?

- code: (1 - 2).-?
- code: (2 - 2).-!
- code: (2 - 1).+?
- code: (2 - 2).+!
- code: nil.~?
- code: false.~!

- code: -'xyz'.# == 3
- code: (1 .. 3).# == 3
- code: nil.# == 0
- code: ().# == 0
- code: -[].# == 0
- code: -{}.# == 0

- code: (1 .. 3).#.?    # .#?
- code: ().#.!          # .#!
# - code: ().#?~.~?
- code: a(3).--.2?
- code: a(2).--.1?
- code: a(-1).++.0?
- code: -'abc'.#.?      # .#?
- code: -''.#.!         # .#!
- code: -''.#.0?        # .#0?
- code: -'a'.#.1?       # .#1?
- code: -'ab'.#.2?      # .#2?

- code: -[].#++.1?
- code: -[1 2 3].#--.2?
- code: -[].#.+?.!!     # .#+?
- code: -[1].#.+?       # .#+?

- code: a(41).++ == 42
- code: a(43).-- == 42

- code: (1 .. 10).^.1?
- code: (1 .. 10).^*.^.2?                       # .^2
- code: (1 .. 10).^* == (2 .. 10)
- code: ().^.~?
- code: (10 .. 1).$.1?
- code: (10 .. 1).*$.$.2?
- code: (10 .. 1).*$ == (10 .. 2)
- code: ().$.~?

- code: -"$base/test/dot-ops.t".<.lines().#.?   # .#?

- code: -'42'.># == 42
- code: -'42.5'.># == 42.5
- code: a(42.5).>I == 42
- code: -'42.5'.>I == 42
- code: -'42.5'.>F == 42.5
- code: a(42).str() == '42'                     # .>$
- code: -{1 2 3 4}.>@ == [1 2 3 4]
- code: list(1 2 3 4).>@ == [1 2 3 4]
- code: list(1 2 3 4).>% == {1 2 3 4}
- code: list(1 2 3 4).>{} == \{1 2 3 4}
- code: (42 - 84).abs() == 42                   # .>+
- code: \'(1 2 [3 4] [5 [6]] (7 8)).@__ == [1 2 3 4 5 6 7 8]
- code: \'(1 2 [3 4] [5 [6]] (7 8)).@_ == [1 2 3 4 5 [6] 7 8]
- code: (1 .. 10).@< == (10 .. 1)
- code: ().@< == ()
- code: ().@< == []
- code: (1 .. 10).>@.vector?()
- code: (1 .. 10).@<.vector?()
- code: -{:a 1 :b 2}.@< == [2 :b 1 :a]
- code: -'abcde'.$@.@<.@$ == 'edcba'
- code: (3 .. 9).@+ == 42
- code: a(3456789).>@.@+ == 42

# - code: a(5).**2 == 25
# - code: a(5).*2 == 10
# - code: a(5)./2 == 2
# - code: a(5).%2 == 1
# - code: a(5).+2 == 7
# - code: a(5).-2 == 3
- code: a(5).@*.take(5) == [5 5 5 5 5]
- code: -[3 5 7].@*.take(7) == [3 5 7 3 5 7 3]
- code: -' Hello '.trimr() == ' Hello'          # .>-
- code: -' Hello '.triml() == 'Hello '          # .<-
- code: -' Hello '.trim() == 'Hello'            # .<->

done:
