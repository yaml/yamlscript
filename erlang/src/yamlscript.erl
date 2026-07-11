-module(yamlscript).
-on_load(load_nif/0).

-export([load/1, load_json/1]).
-export([nif_load_json/1]).

load_nif() ->
  Priv = filename:join(filename:dirname(code:which(?MODULE)), "../priv"),
  erlang:load_nif(filename:join(Priv, "yamlscript_nif"), 0).

load(Input) when is_list(Input) ->
  load(list_to_binary(Input));
load(Input) when is_binary(Input) ->
  case load_json(Input) of
    {error, Message} ->
      {error, Message};
    JSON ->
      Resp = json:decode(JSON),
      case maps:get(<<"error">>, Resp, null) of
        null ->
          {ok, maps:get(<<"data">>, Resp)};
        Error ->
          {error, maps:get(<<"cause">>, Error)}
      end
  end.

load_json(Input) when is_list(Input) ->
  load_json(list_to_binary(Input));
load_json(Input) when is_binary(Input) ->
  nif_load_json(Input).

nif_load_json(_Input) ->
  erlang:nif_error(nif_not_loaded).
