use YAMLScript::Test;

expr '
(1 + 2)','
(+ 1 2)';

expr
'prn(123 456)','
(prn 123 456)';

expr '
if (a = b): [a]','
(if (= a b) a)';

expr
'prn(123) prn(456)','
(do (prn 123) (prn 456))';
