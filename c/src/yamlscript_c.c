#include "yamlscript_c.h"
#include <string.h>
#include <dlfcn.h>
#include <stdlib.h>

typedef const char* (*pfn_load_ys_to_json)(void *isolate_thread, const char *buf);

typedef struct
{
    void *handle;
    pfn_load_ys_to_json load_ys_to_json;
}  yslibdata_;

static yslibdata_ yslibdata = {0};

#define ys_errif(cond, ec) do { if(cond) return (yamlscript_##ec); } while(0)
#define ys_call(call) do { yamlscript_errcode ec = (call); if(ec) return ec; } while(0)


static yamlscript_errcode
mklibname(char *buf, size_t len)
{
    #define appendstr(str_)                 \
    {                                       \
        const char* str = str_;             \
        size_t slen = strlen(str);          \
        if(pos + slen >= len)               \
            return yamlscript_small_buffer; \
        if(slen)                            \
            memcpy(buf + pos, str, slen);   \
    }
    size_t pos = 0;
    appendstr("libyamlscript.");
    appendstr("so."); // FIXME
    appendstr(YAMLSCRIPT_VERSION_STR);
    buf[pos] = '\0';
    return yamlscript_success;
    #undef appendstr
}

static yamlscript_errcode
yamlscript_init()
{
    char libname[128] = {0};
    ys_errif(mklibname(libname, sizeof(libname)), null_arg);
    yslibdata.handle = dlopen(libname, RTLD_LAZY);
    ys_errif(!yslibdata.handle, lib_error);
    dlerror();    /* Clear any existing error */
    *(void**)&yslibdata.load_ys_to_json = (pfn_load_ys_to_json)dlsym(yslibdata.handle, "load_ys_to_json");
    ys_errif(dlerror(), lib_error);
    return yamlscript_success;
}

yamlscript_errcode
yamlscript_load_ys_to_json(const char *ys,
                           char *json,
                           int json_size,
                           int *json_size_required)
{
    ys_errif(!ys, null_arg);
    ys_errif(!json, null_arg);
    ys_errif(json_size && !json, bad_arg);
    ys_errif(!json_size_required, null_arg);
    if(!yslibdata.handle)
        ys_call(yamlscript_init());
    const char *wtf = yslibdata.load_ys_to_json(NULL, ys);
    int len = (int)strlen(wtf);
    *json_size_required = len + 1;
    if(*json_size_required <= json_size) {
        memcpy(json, wtf, len);
        json[len] = '\0';
    }
    free((void*)wtf);
    return yamlscript_success;
}
