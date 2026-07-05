defmodule YAMLScript do
  # Copyright 2023-2026 Ingy dot Net
  # This code is licensed under MIT license (See License for details)

  @moduledoc """
  Elixir binding/API for the libys shared library.

  This module is an Elixir port of the Python 'yamlscript' module,
  the reference implementation for YAMLScript FFI bindings to libys.

  The `load/1` function takes a YAMLScript string as input and
  returns `{:ok, data}` where data is the value the YAMLScript code
  evaluates to, or `{:error, message}`. The `load!/1` variant returns
  the data directly and raises `YAMLScript.Error` on failure.
  """

  @on_load :load_nif

  @doc false
  def load_nif do
    path = :filename.join(:code.priv_dir(:yamlscript), ~c"yamlscript_nif")
    :erlang.load_nif(path, 0)
  end

  @doc """
  Compile and eval a YAMLScript string and return the result.
  """
  @spec load(String.t()) :: {:ok, term()} | {:error, String.t()}
  def load(input) when is_binary(input) do
    case nif_load_ys_to_json(input) do
      {:error, message} ->
        {:error, message}

      json when is_binary(json) ->
        # Decode the JSON response and check for a libys error:
        resp = JSON.decode!(json)

        cond do
          err = resp["error"] ->
            {:error, err["cause"]}

          Map.has_key?(resp, "data") ->
            {:ok, resp["data"]}

          true ->
            {:error, "Unexpected response from 'libys'"}
        end
    end
  end

  @doc """
  Like `load/1` but returns the data directly and raises
  `YAMLScript.Error` on failure.
  """
  @spec load!(String.t()) :: term()
  def load!(input) do
    case load(input) do
      {:ok, data} -> data
      {:error, message} -> raise YAMLScript.Error, message: message
    end
  end

  defp nif_load_ys_to_json(_input) do
    :erlang.nif_error(:nif_not_loaded)
  end
end

defmodule YAMLScript.Error do
  @moduledoc "Error raised by `YAMLScript.load!/1`."
  defexception [:message]
end
