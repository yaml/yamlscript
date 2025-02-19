#include "ysparse_edn.hpp"
#include <string.h>


#if defined(__cplusplus)
extern "C" {
#endif
// see
// https://stackoverflow.com/questions/230689/best-way-to-throw-exceptions-in-jni-code
// https://stackoverflow.com/questions/4138168/what-happens-when-i-throw-a-c-exception-from-a-native-java-method

namespace {
C4_NORETURN void ys2edn_parse_error(const char* msg, size_t msg_len, Location location, void *user_data)
{
    YsParseError exc;
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
    _RYML_CB_FREE(get_callbacks(), ryml2edn, Ryml2Edn, 1);
}

RYML_EXPORT size_type ys2edn_parse(Ryml2Edn *ryml2edn,
                                   const char *filename,
                                   char *ys, size_type ys_size,
                                   char *edn, size_type edn_size)
{
    TIMED_SECTION("ys2edn_parse", ys_size);
    csubstr filename_ = filename ? to_csubstr(filename) : csubstr{};
    substr ys_(ys, (size_t)ys_size);
    {
        TIMED_SECTION("reset + reserve");
        ryml2edn->reset();
        ryml2edn->m_handler.reserve(ys_size > edn_size ? 3 * ys_size : edn_size, 256u);
    }
    {
        TIMED_SECTION("parse_in_place", ys_size);
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
