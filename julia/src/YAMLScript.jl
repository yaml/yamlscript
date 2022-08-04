module YAMLScript

import YAML

include("Function.jl")

mutable struct new
  file; str; data
  fromFile; fromString; compile; run

  function new()
    this = new()

    this.fromFile = function(file)
      this.file = file
      this.data = YAML.load_file(file)
      this.compile()
      return this
    end

    this.fromString = function(str)
      this.file = file
      this.data = YAML.load(str)
      this.compile()
      return this
    end

    this.compile = function()
      for (k, v) in this.data
        m = match(r"^([a-z]+)\((.*)\)$", k)
        isnothing(m) &&
          error("Unsupported YAMLScript key '$(k)'")

        name = string(m[1])
        sign = map(string, split(m[2], r"\s*,\s*"))
        body = v
        ns[name] = Func(name, sign, body)
      end

      return this
    end

    this.run = function(argv...)
      main = ns["main"]
      args = argv[1]
      main.call(ns, args)
      return this
    end

    return this
  end
end

# Define the global namespace:
ns = Dict()

ns["say"] = function(str)
  println(str)
end

end
