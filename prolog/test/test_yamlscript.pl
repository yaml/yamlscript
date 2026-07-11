:- use_module('../src/yamlscript.pl').

run :-
    load_json("!ys-0:\ntest:: inc(41)", JSON),
    sub_atom(JSON, _, _, _, '"test":42'),
    write('ok - load ys code'), nl.
