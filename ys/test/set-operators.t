#!/usr/bin/env ys-0

use ys::taptest: :all

test::
- code: (\{1 2 3} + \{3 4 5})
  want:: \{1 2 3 4 5}

- code: (\{1 2 3} - \{3 4 5})
  want:: \{1 2}

- code: (\{1 2 3} * \{3 4 5})
  want:: \{3}

- code: (\{1 2 3} + [3 4 5])
  want:: \{1 2 3 4 5}

- code: (\{1 2 3} - [3 4 5])
  want:: \{1 2}

- code: (\{1 2 3} * [3 4 5])
  want:: \{3}

- code: (\{1 2 3} + ql(3 4 5))
  want:: \{1 2 3 4 5}

- code: (\{1 2 3} - ql(3 4 5))
  want:: \{1 2}

- code: (\{1 2 3} * ql(3 4 5))
  want:: \{3}

- code: ([1 2 3] + \{3 4 5}).to-set() == \{1 2 3 4 5}

- code: ([1 2 3] + \{3 4 5}):count == 6

- code: take(3 ([1 2 3] + \{3 4 5})) == ql(1 2 3)

- code: ([1 2 3] + \{3 4 5}):to-type == "vec"

- code: ([1 2 3] - \{3 4 5})
  want:: +[1 2]

- code: ([1 2 3] - \{3 4 5}):to-type == "vec"

- code: ([1 2 3] * \{3 4 5})
  want:: +[3]

- code: ([1 2 3] * \{3 4 5}):to-type == "vec"

- code: (ql(1 2 3) + \{3 4 5}).to-set() == \{1 2 3 4 5}

- code: (ql(1 2 3) + \{3 4 5}):count == 6

- code: take(3 (ql(1 2 3) + \{3 4 5})) == ql(1 2 3)

- code: (ql(1 2 3) + \{3 4 5}):to-type == "seq"

- code: (ql(1 2 3) - \{3 4 5})
  want:: ql(1 2)

- code: (ql(1 2 3) - \{3 4 5}):to-type == "seq"

- code: (ql(1 2 3) * \{3 4 5})
  want:: ql(3)

- code: (ql(1 2 3) * \{3 4 5}):to-type == "seq"

- code: ((1 .. 3) + \{3 4 5}).to-set() == \{1 2 3 4 5}

- code: ((1 .. 3) + \{3 4 5}):count == 6

- code: take(3 ((1 .. 3) + \{3 4 5})) == ql(1 2 3)

- code: ((1 .. 3) + \{3 4 5}):to-type == "seq"

- code: ((1 .. 3) - \{3 4 5})
  want:: ql(1 2)

- code: ((1 .. 3) - \{3 4 5}):to-type == "seq"

- code: ((1 .. 3) * \{3 4 5})
  want:: ql(3)

- code: ((1 .. 3) * \{3 4 5}):to-type == "seq"

- code: ([3 1 3 2] - \{2})
  want:: +[3 1 3]

- code: ([3 1 3 2] * \{3})
  want:: +[3 3]

- code: (ql(3 1 3 2) * \{3})
  want:: ql(3 3)

- code: ([nil false 1 nil false] * \{nil false})
  want:: +[nil false nil false]

- code: ([nil false 1 nil false] - \{nil false})
  want:: +[1]

- code: (\{nil false 1} * [nil false])
  want:: \{nil false}

- code: (\{nil false 1} - [nil false])
  want:: \{1}

- code: ([1 2] + \{})
  want:: +[1 2]

- code: ([] + \{1})
  want:: +[1]

- code: (\{} + [1 1])
  want:: \{1}

- code: (\{1 2} - [])
  want:: \{1 2}

- code: ([1 2] * \{})
  want:: +[]

- code: (ql(1 2) * \{})
  want:: ql()

- code: (\{1 2} * [])
  want:: \{}

- code: (\{1 2} - \{1 2})
  want:: \{}

- code: (\{[1 2] 3} - \{[1 2]})
  want:: \{3}

- code: ([[1 2] [3]] * \{[1 2]})
  want:: +[[1 2]]

- code: (\{1 2} - 2)
  want:: \{1}

- code: (\{nil false 1} - nil - false)
  want:: \{1}

- code: (\{1} + [2] + ql(3))
  want:: \{1 2 3}

- code: ([1 2 3] - \{2} - \{3})
  want:: +[1]

- code: ([3 1 3 2] * \{1 3} * \{3})
  want:: +[3 3]

- code: ([1 [2]] - [2])
  want:: +[1]

- code: ([1 2] + [2 3])
  want:: +[1 2 2 3]

- code: ([1 2] * 2)
  want:: +[1 2 1 2]

- code: (2 * [1 2])
  want:: +[1 2 1 2]

- code: (ql(1 2) * 2)
  want:: ql(1 2 1 2)

- code: ("ab" * 2)
  want:: "abab"

- code: ("a" + "b")
  want:: "ab"

- code: (\{1} + (\{2} * \{2 3}))
  want:: \{1 2}

- code: (\{} < \{})
  want:: false

- code: (\{} <= \{})
  want:: true

- code: (\{} > \{})
  want:: false

- code: (\{} >= \{})
  want:: true

- code: (\{} < \{1})
  want:: true

- code: (\{} <= \{1})
  want:: true

- code: (\{} > \{1})
  want:: false

- code: (\{} >= \{1})
  want:: false

- code: (\{1} < \{1})
  want:: false

- code: (\{1} <= \{1})
  want:: true

- code: (\{1} > \{1})
  want:: false

- code: (\{1} >= \{1})
  want:: true

- code: (\{1} < \{1 2})
  want:: true

- code: (\{1} <= \{1 2})
  want:: true

- code: (\{1} > \{1 2})
  want:: false

- code: (\{1} >= \{1 2})
  want:: false

- code: (\{1 2} < \{1})
  want:: false

- code: (\{1 2} <= \{1})
  want:: false

- code: (\{1 2} > \{1})
  want:: true

- code: (\{1 2} >= \{1})
  want:: true

- code: (\{1} < \{2})
  want:: false

- code: (\{1} <= \{2})
  want:: false

- code: (\{1} > \{2})
  want:: false

- code: (\{1} >= \{2})
  want:: false

- code: (\{1 2} < \{2 3 4})
  want:: false

- code: (\{1 2} <= \{2 3 4})
  want:: false

- code: (\{1 2} > \{2 3 4})
  want:: false

- code: (\{1 2} >= \{2 3 4})
  want:: false

- code: (\{nil} < \{nil false})
  want:: true

- code: (\{nil} <= \{nil false})
  want:: true

- code: (\{nil} > \{nil false})
  want:: false

- code: (\{nil} >= \{nil false})
  want:: false

- code: lt(\{} \{1} \{1 2})

- code: lt(\{1}).call(\{})

- code: \{} < \{1} < \{1 2}

- code: lt(\{} \{1} \{99})
  want:: false

- code: le(\{1} \{1} \{1 2})

- code: le(\{1}).call(\{1})

- code: \{1} <= \{1} <= \{1 2}

- code: le(\{1} \{1} \{99})
  want:: false

- code: gt(\{1 2} \{1} \{})

- code: gt(\{1}).call(\{1 2})

- code: \{1 2} > \{1} > \{}

- code: gt(\{1 2} \{1} \{99})
  want:: false

- code: ge(\{1 2} \{1} \{1})

- code: ge(\{1}).call(\{1 2})

- code: \{1 2} >= \{1} >= \{1}

- code: ge(\{1 2} \{1} \{99})
  want:: false

- code: (\{1} < \{1})
  want:: false

- code: (\{1} > \{1})
  want:: false

done:
