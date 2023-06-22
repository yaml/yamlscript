use Test::More;

use YAMLScript;

my $ys = YAMLScript->new;

is $ys->rep("(+ 1 2 3 4 5)"), 15,
    "Eval YAMLScript from Perl works";

is $ys->rep("(defn add2 [x y] (+ x y))"), 'user/add2',
    "Defined a YAMLScript (Lingy) function";

is $ys->rep("(add2 5 6)"), 11,
    "Called our defined Lingy function";

eval { $ys->rep("(add2 5)") };
is $@, "YAMLScript Error: Wrong number of args (1) passed to function\n",
    "Error, too few args to our Lingy function";

eval { $ys->rep("(add2 5 6 7)") };
is $@, "YAMLScript Error: Wrong number of args (3) passed to function\n",
    "Error, too many args to our Lingy function";

my $form = $ys->read('(+ 1 2 3)');

is ref($form), 'Lingy::List',
    '$ys->read works';

my $print = $ys->print($form);

is $print, '(+ 1 2 3)',
    '$ys->print works';

my $result = $ys->eval($form);
is ref($result), 'Lingy::Number',
    '$ys->eval($form) returns a Lingy form';
is $ys->print($result), 6,
    '$ys->eval result is correct';

is $result->unbox, 6,
    '$ys->eval result supports ->unbox';

done_testing;
