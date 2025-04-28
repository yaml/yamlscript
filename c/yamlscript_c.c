#include "yamlscript_c.h"
#include <string.h>
#include <dlfcn.h>

typedef void (*pfn_ys2json)(void *isolate_thread, const char *buf);

typedef struct
{
    void *handle;
    pfn_ys2json ys2json;
}  yslibdata_;

static yslibdata_ yslibdata = {0};

#define ys_errif(cond, ec) do { if(cond) return (yamlscript_##ec); } while(0)


static yamlscript_errcode
mklibname(char *buf, size_t len)
{
    #define appendstr(str_)                     \
        {                                       \
            const char* str = str_;             \
            size_t slen = strlen(str);          \
            if(pos + slen >= len)               \
                return yamlscript_small_buffer; \
            memcpy(buf + pos, str, slen);       \
        }
    size_t pos = 0;
    appendstr("libyamlscript.");
    appendstr("so."); // FIXME
    appendstr(YAMLSCRIPT_VERSION_STR);
    buf[pos] = '\0';
    return yamlscript_success;
}

yamlscript_errcode
yamlscript_init(yamlscript_lib *lib)
{
    ys_errif(lib, null_arg);
    char libname[128] = {0};
    ys_errif(mklibname(libname, sizeof(libname)), null_arg);
    yslibdata.handle = dlopen(libname, RTLD_LAZY);
    ys_errif(!yslibdata.handle, lib_error);
    dlerror();    /* Clear any existing error */
    *(void**)&yslibdata.ys2json = (pfn_ys2json)dlsym(yslibdata.handle,
                                                     "load_ys_to_json");
    ys_errif(dlerror(), lib_error);
    *lib = &yslibdata;
    return yamlscript_success;
}

yamlscript_errcode
yamlscript_terminate(const yamlscript_lib lib)
{
    ys_errif(!lib, null_arg);
    ys_errif(lib != &yslibdata, bad_init);
    dlclose(yslibdata.handle);
    ys_errif(dlerror(), lib_error);
    return yamlscript_success;
}

yamlscript_errcode
yamlscript_load_ys_to_json(yamlscript_lib lib,
                           yamlscript_buffer_ro_t const* ys,
                           yamlscript_buffer_rw_t const* json,
                           int *json_size)
{
    ys_errif(!lib, null_arg);
    ys_errif(lib != &yslibdata, bad_init);
    ys_errif(!ys, null_arg);
    ys_errif(!json, null_arg);
    ys_errif(!json_size, null_arg);
    ys_errif(ys->size && !ys->buf, bad_arg);
    ys_errif(json->size && !json->buf, bad_arg);
    return yamlscript_success;
}
