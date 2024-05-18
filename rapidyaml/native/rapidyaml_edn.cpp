#define RYML_SINGLE_HDR_DEFINE_NOW
#include <ryml_all.hpp>
#include "rapidyaml_edn.hpp"
#include <string.h>

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml

using namespace ryml;

#if 0
#define TIMED_SECTION_INNER(name) YS2EDN_TIMED
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
        fflush(stderr);
    }
};
#endif


#if defined(__cplusplus)
extern "C" {
#endif

RYML_EXPORT Ryml2Edn *ys2edn_init()
{
    TIMED_SECTION("ys2edn_init");
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
    size_type required_size = 1 + (size_type)result.size();
    if(required_size <= edn_size)
    {
        memcpy(edn, result.str, (size_t)edn_size);
        edn[edn_size] = '\0';
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

RYML_EXPORT char *ys2edn_stateless(const char *filename,
                                   char *ys, size_type ys_size)
{
    Ryml2Edn ryml2edn;
    return ys2edn_alloc(&ryml2edn, filename, ys, ys_size);
}

#if defined(__cplusplus)
}
#endif
