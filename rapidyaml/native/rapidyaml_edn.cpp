#define RYML_SINGLE_HDR_DEFINE_NOW
#include <rapidyaml_all.hpp>
#include "rapidyaml_edn.hpp"
#include <string.h>

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml

using namespace ryml;

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
// see
// https://stackoverflow.com/questions/230689/best-way-to-throw-exceptions-in-jni-code
// https://stackoverflow.com/questions/4138168/what-happens-when-i-throw-a-c-exception-from-a-native-java-method

namespace {
C4_NORETURN void ys2edn_parse_error(const char* msg, size_t msg_len, Location location, void *user_data)
{
    Ryml2EdnParseError exc;
    exc.location = location;
    exc.msg.assign(msg, msg_len);
    throw exc;
}
} // anon namespace

RYML_EXPORT Ryml2Edn *ys2edn_init()
{
    TIMED_SECTION("ys2edn_init");
    Callbacks cb = {};
    cb.m_error = &ys2edn_parse_error;
    set_callbacks(cb);
    Ryml2Edn *ryml2edn = _RYML_CB_ALLOC(get_callbacks(), Ryml2Edn, 1);
    _RYML_CB_CHECK(get_callbacks(), ryml2edn != nullptr);
    new ((void*)ryml2edn) Ryml2Edn();
    return ryml2edn;
}

RYML_EXPORT void ys2edn_destroy(Ryml2Edn *ryml2edn)
{
    TIMED_SECTION("ys2edn_destroy");
    ryml2edn->~Ryml2Edn();
}

RYML_EXPORT size_type ys2edn_parse(Ryml2Edn *ryml2edn,
                                   const char *filename,
                                   char *ys, size_type ys_size,
                                   char *edn, size_type edn_size)
{
    TIMED_SECTION("ys2edn_parse");
    csubstr filename_ = filename ? to_csubstr(filename) : csubstr{};
    substr ys_(ys, (size_t)ys_size);
    {
        TIMED_SECTION("reset");
        ryml2edn->reset();
    }
    {
        TIMED_SECTION("parse_in_place");
        ryml2edn->m_parser.parse_in_place_ev(filename_, ys_);
    }
    return ys2edn_retry_get(ryml2edn, edn, edn_size);
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

#if defined(__cplusplus)
}
#endif
