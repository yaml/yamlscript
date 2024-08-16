#define RYML_SINGLE_HDR_DEFINE_NOW
#include <rapidyaml_all.hpp>
#include "rapidyaml_edn.hpp"
#include <string.h>
#include <jni.h>

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml

using namespace ryml;

#if 0
#define TIMED_SECTION_INNER(name) TIMED_SECTION(name)
#else
#define TIMED_SECTION_INNER(name)
#endif

#ifndef YS2EDN_TIMED
#define TIMED_SECTION(name)
#else
#include <chrono>
#define TIMED_SECTION(name) timed_section C4_XCAT(ts, __LINE__)(name)
struct timed_section
{
    using myclock = std::chrono::steady_clock;
    using fmsecs = std::chrono::duration<double, std::milli>;
    csubstr name;
    myclock::time_point start;
    fmsecs since() const { return myclock::now() - start; }
    timed_section(csubstr n) : name(n), start(myclock::now()) {}
    ~timed_section()
    {
        fprintf(stderr, "%.6fms: %.*s\n", since().count(), (int)name.len, name.str);
    }
};
#endif


#if defined(__cplusplus)
extern "C" {
#endif

jint throwRuntimeExceptionError(JNIEnv *env, const char *message)
{
    fprintf(stderr, "here 1, '%s'\n", message);fflush(stdout);
    const char *className = "java/lang/RuntimeException";
    jclass exClass = env->FindClass(className);
    fprintf(stderr, "here 2, '%p'\n", exClass);fflush(stdout);
    //if (exClass == NULL) {
    //    return throwRuntimeExceptionError(env, className);
    //}
    return env->ThrowNew(exClass, message);
}

void onRapidyamlError(const char* msg, size_t msg_len, Location location, void *user_data)
{
fprintf(stderr, "here 0, '%.*s' '%s'\n", (int)msg_len, msg, msg);fflush(stdout);
    JNIEnv *env = (JNIEnv*)user_data;
    // msg may not be zero-terminated; ensure we terminate it:
    char *zmsg_ = malloc(msg_len + 1u);
    const char *zmsg = zmsg_;
    if (zmsg) {
        memcpy(zmsg, msg, msg_len);
        msg[msg_len] = '\0';
    }
    else {
        zmsg = msg;
    }
    (void)throwRuntimeExceptionError(env, zmsg);
}

RYML_EXPORT Ryml2Edn *ys2edn_init(JNIEnv *env)
{
    TIMED_SECTION("ys2edn_init");
fprintf(stderr, "wtf init 0\n");fflush(stdout);
    if(env)
    {
        c4::yml::Callbacks cb = {};
        cb.m_user_data = env;
        cb.m_error = &onRapidyamlError;
        c4::yml::set_callbacks(cb);
    }
fprintf(stderr, "init 0\n");fflush(stdout);
    Ryml2Edn *ryml2edn = _RYML_CB_ALLOC(get_callbacks(), Ryml2Edn, 1);
    _RYML_CB_CHECK(get_callbacks(), ryml2edn != nullptr);
fprintf(stderr, "init 1\n");fflush(stdout);
    new ((void*)ryml2edn) Ryml2Edn(env);
fprintf(stderr, "init 2\n");fflush(stdout);
    return ryml2edn;
}

RYML_EXPORT void ys2edn_destroy(Ryml2Edn *ryml2edn)
{
    TIMED_SECTION("ys2edn_destroy");
    ryml2edn->~Ryml2Edn();
}

RYML_EXPORT size_type ys2edn(Ryml2Edn *ryml2edn,
                             const char *filename,
                             char *ys, size_type ys_size,
                             char *edn, size_type edn_size)
{
    TIMED_SECTION("ys2edn");
    csubstr filename_ = to_csubstr(filename);
    substr ys_(ys, (size_t)ys_size);
    {
        TIMED_SECTION_INNER("reset");
        ryml2edn->reset();
    }
    {
        TIMED_SECTION_INNER("parse_in_place");
        ryml2edn->m_parser.parse_in_place_ev(filename_, ys_);
    }
    return ys2edn_retry_get(ryml2edn, edn, edn_size);
}

RYML_EXPORT size_type ys2edn_failsmall(Ryml2Edn *ryml2edn,
                                       const char *filename,
                                       char *ys, size_type ys_size,
                                       char *edn, size_type edn_size)
{
    size_type sz = ys2edn(ryml2edn, filename, ys, ys_size, edn, edn_size);
    if(sz > edn_size)
    {
        _RYML_CB_ERR(get_callbacks(), "edn buffer too small");
    }
    return sz;
}

RYML_EXPORT size_type ys2edn_retry_get(Ryml2Edn *ryml2edn,
                                       char *edn, size_type edn_size)
{
    TIMED_SECTION("ys2edn_retry_get");
    csubstr result = to_csubstr(ryml2edn->m_sink.result);
    size_type required_size = 1 + (size_type)result.len;
    if(required_size <= edn_size)
    {
        memcpy(edn, result.str, (size_t)result.len);
        edn[result.len] = '\0';
    }
    else if(edn_size > 0)
    {
        edn[0] = '\0';
    }
    return required_size;
}

RYML_EXPORT char * ys2edn_alloc(Ryml2Edn *ryml2edn,
                                const char *filename,
                                char *ys, size_type ys_size)
{
    TIMED_SECTION("ys2edn_alloc");
    csubstr filename_ = to_csubstr(filename);
    substr ys_(ys, (size_t)ys_size);
    {
        TIMED_SECTION_INNER("reset");
        ryml2edn->reset();
    }
    {
        TIMED_SECTION_INNER("parse_in_place");
        ryml2edn->m_parser.parse_in_place_ev(filename_, ys_);
    }
    csubstr result = to_csubstr(ryml2edn->m_sink.result);
    char *edn;
    {
        TIMED_SECTION_INNER("alloc");
        edn = _RYML_CB_ALLOC(get_callbacks(), char, result.len + 1);
        _RYML_CB_CHECK(get_callbacks(), edn != nullptr);
    }
    {
        TIMED_SECTION_INNER("memcpy");
        if(result.len)
        {
            memcpy(edn, result.str, result.len);
        }
        edn[result.len] = '\0';
    }
    return edn;
}

RYML_EXPORT void ys2edn_free(char *edn)
{
    _RYML_CB_FREE(get_callbacks(), edn, char, strlen(edn) + 1);
}

#if defined(__cplusplus)
}
#endif
