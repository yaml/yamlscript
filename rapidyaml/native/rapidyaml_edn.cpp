#define RYML_SINGLE_HDR_DEFINE_NOW
#include <ryml_all.hpp>
#include "rapidyaml_edn.hpp"
#include <string.h>

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml


#if defined(__cplusplus)
extern "C" {
#endif

RYML_EXPORT Ryml2Edn *ys2edn_init()
{
    Ryml2Edn *ryml2edn = _RYML_CB_ALLOC(c4::yml::get_callbacks(), Ryml2Edn, 1);
    _RYML_CB_CHECK(c4::yml::get_callbacks(), ryml2edn != nullptr);
    new ((void*)ryml2edn) Ryml2Edn();
    return ryml2edn;
}

RYML_EXPORT void ys2edn_destroy(Ryml2Edn *ryml2edn)
{
    ryml2edn->~Ryml2Edn();
}

RYML_EXPORT size_type ys2edn(Ryml2Edn *ryml2edn,
                             const char *filename,
                             char *ys, size_type ys_size,
                             char *edn, size_type edn_size)
{
    c4::csubstr filename_ = c4::to_csubstr(filename);
    c4::substr ys_(ys, (size_t)ys_size);
    ryml2edn->reset();
    ryml2edn->m_parser.parse_in_place_ev(filename_, ys_);
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
        _RYML_CB_ERR(c4::yml::get_callbacks(), "edn buffer too small");
    }
    return sz;
}

RYML_EXPORT size_type ys2edn_retry_get(Ryml2Edn *ryml2edn,
                                       char *edn, size_type edn_size)
{
    c4::csubstr result = c4::to_csubstr(ryml2edn->m_sink.result);
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
    c4::csubstr filename_ = c4::to_csubstr(filename);
    c4::substr ys_(ys, (size_t)ys_size);
    ryml2edn->reset();
    ryml2edn->m_parser.parse_in_place_ev(filename_, ys_);
    c4::csubstr result = c4::to_csubstr(ryml2edn->m_sink.result);
    char *edn = _RYML_CB_ALLOC(c4::yml::get_callbacks(), char, result.len + 1);
    _RYML_CB_CHECK(c4::yml::get_callbacks(), edn != nullptr);
    if(result.len)
    {
        memcpy(edn, result.str, result.len);
    }
    edn[result.len] = '\0';
    return edn;
}

RYML_EXPORT void ys2edn_free(char *edn)
{
    _RYML_CB_FREE(ryml::get_callbacks(), edn, char, strlen(edn) + 1);
}

RYML_EXPORT char *ys2edn_stateless(const char *filename,
                                   char *ys, size_type ys_size)
{
    printf("aqui 0\n");
    Ryml2Edn ryml2edn;
    printf("aqui 1\n");
    return ys2edn_alloc(&ryml2edn, filename, ys, ys_size);
}

#if defined(__cplusplus)
}
#endif
