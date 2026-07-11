-module(yamlscript_test).
-export([run/0]).

run() ->
  {ok, #{<<"test">> := 42}} =
    yamlscript:load(<<"!ys-0:\ntest:: inc(41)">>),
  io:format("ok - load ys code~n").
