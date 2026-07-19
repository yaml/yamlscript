defmodule YAMLScript.MixProject do
  use Mix.Project

  # This value is automatically updated by 'make bump':
  @version "0.2.29"

  def project do
    [
      app: :yamlscript,
      version: @version,
      elixir: "~> 1.18",
      compilers: [:elixir_make] ++ Mix.compilers(),
      make_makefile: "Makefile.nif",
      deps: deps(),
      description:
        "Load YAML files with optional YS functional programming",
      package: package(),
      source_url: "https://github.com/yaml/yamlscript",
      # Keep generated docs out of doc/, which holds the tracked
      # readme fragments:
      docs: [main: "YAMLScript", output: ".docs"]
    ]
  end

  def application do
    [extra_applications: []]
  end

  defp deps do
    [
      {:elixir_make, "~> 0.8", runtime: false},
      {:ex_doc, "~> 0.34", only: :dev, runtime: false}
    ]
  end

  defp package do
    [
      licenses: ["MIT"],
      links: %{
        "Website" => "https://yamlscript.org",
        "GitHub" => "https://github.com/yaml/yamlscript"
      },
      files: ~w(
        lib c_src Makefile.nif mix.exs README.md LICENSE
      )
    ]
  end
end
