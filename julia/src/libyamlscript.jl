module libyamlscript

import Base.Libc: Libdl

const YAMLSCRIPT_VERSION = "0.1.81"
const libyamlscript_name = "libyamlscript.$(Libdl.dlext).$(YAMLSCRIPT_VERSION)"
const libhandle = Ref{Ptr{Cvoid}}()
const graal_create_isolate_fptr = Ref{Ptr{Cvoid}}()
const graal_tear_down_isolate_fptr = Ref{Ptr{Cvoid}}()
const load_ys_to_json_fptr = Ref{Ptr{Cvoid}}()

function graal_create_isolate(params, isolate, thread)
    ccall(graal_create_isolate_fptr[],
          Cint, (Ptr{Cvoid}, Ptr{Ptr{Cvoid}}, Ptr{Ptr{Cvoid}}),
          params, isolate, thread)
end

function graal_tear_down_isolate(thread)
    ccall(graal_tear_down_isolate_fptr[], Cint, (Ptr{Cvoid},), thread)
end

function load_ys_to_json(thread, script::String)
    ccall(load_ys_to_json_fptr[], Cstring, (Ptr{Cvoid}, Cstring), thread, script)
end

function _library_not_found_error(libname)
    msg = """
Shared library file `$(libname)` not found
Try: curl https://yamlscript.org/install | VERSION=$(YAMLSCRIPT_VERSION) LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript"""
    error(msg)
end

function init()
    libpath = Libdl.find_library(libyamlscript_name)
    if libpath == ""
        _library_not_found_error(libyamlscript_name)
    end

    libhandle[] = Libdl.dlopen(libpath, Libdl.RTLD_LAZY | Libdl.RTLD_LOCAL)
    graal_create_isolate_fptr[] = Libdl.dlsym(libhandle[], :graal_create_isolate)
    graal_tear_down_isolate_fptr[] = Libdl.dlsym(libhandle[], :graal_tear_down_isolate)
    load_ys_to_json_fptr[] = Libdl.dlsym(libhandle[], :load_ys_to_json)
end

end  # module libyamlscript
