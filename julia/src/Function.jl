function val(str, ns)
  if typeof(str) != String
    return str
  end

  if !occursin(r"\$", str)
    return str
  end

  return replace(str, r"\$(\w+)" =>
    function(m) return ns[m[2:end]] end
  )
end

mutable struct Func
  name
  sign
  body

  call

  function Func(name, sign, body)
    this = new()

    this.name = name
    this.sign = sign
    this.body = body

    this.call = function(ns, args)
      i = 1
      for var in this.sign
        ns[var] = args[i]
        i += 1
      end

      for stmt in this.body
        f = ns[collect(keys(stmt))[1]]
        v = collect(values(stmt))[1]
        f(val(v, ns))
      end
    end

    return this
  end
end
