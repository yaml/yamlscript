defmodule Yamlscript.MixProject do
  use Mix.Project

  def project do
    [
      app: :yamlscript,
      version: "0.0.16",
      description: "Program in YAML",
      elixir: "~> 1.12",
      package: package(),
      deps: deps()
    ]
  end

  def package do
    [
      licenses: ["MIT"],
      files: ~w(lib test mix.exs README*),
      maintainers: ["Ingy döt Net <ingy@ingy.net>"],
      links: %{"GitHub" => "https://github.com/yaml/yamlscript"}
    ]
  end

  defp deps do
    [
      {:ex_doc, ">= 0.0.0", only: :dev, runtime: false}
    ]
  end

end
