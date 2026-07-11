:- module(yamlscript, [load_json/2]).
:- initialization(init_yamlscript).

init_yamlscript :-
    '$dlopen'('priv/libyamlscript_prolog.so', 2, Handle),
    '$register_function'(
        Handle,
        yamlscript_load_json,
        [cstring],
        cstring
    ).

load_json(Input, JSON) :-
    yamlscript_load_json(Input, JSON).
