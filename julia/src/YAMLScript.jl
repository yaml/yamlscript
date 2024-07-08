module YAMLScript

import JSON

include("libyamlscript.jl")

mutable struct Runtime
    isolate::Ref{Ptr{Cvoid}}
    error

    function Runtime()
        ys = new(Ref{Ptr{Cvoid}}(), nothing)
        ys.isolate[] = C_NULL
        return ys
    end
end

function graal_create_isolate(func::Function, params, isolate::Ref{Ptr{Cvoid}}, thread::Ref{Ptr{Cvoid}})
    libyamlscript.init()
    if libyamlscript.graal_create_isolate(params, isolate, thread) != 0
        error("Failed to create isolate")
    end
    try
        return func()
    finally
        if libyamlscript.graal_tear_down_isolate(thread[]) != 0
            error("Failed to tear down isolate")
        end
    end
end

function load(ys::Runtime, code::AbstractString)
    thread = Ref{Ptr{Cvoid}}()
    thread[] = C_NULL

    graal_create_isolate(C_NULL, ys.isolate, thread) do
        json_data = libyamlscript.load_ys_to_json(thread[], code)
        json_src = unsafe_string(json_data)
        resp = JSON.parse(json_src)
        ys.error = get(resp, "error", nothing)
        if ys.error !== nothing
            error(get(ys.error, "cause", ""))
        end
        if haskey(resp, "data")
            return resp["data"]
        else
            error("Unexpected response from 'libyamlscript'")
        end
    end
end

end
