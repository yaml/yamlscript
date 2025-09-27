#include "ysparse_evt.hpp"

using namespace ryml;

#if defined(__cplusplus)
extern "C" {
#endif
// see
// https://stackoverflow.com/questions/230689/best-way-to-throw-exceptions-in-jni-code
// https://stackoverflow.com/questions/4138168/what-happens-when-i-throw-a-c-exception-from-a-native-java-method

namespace {
C4_NORETURN void ysparse_error(const char* msg, size_t msg_len, Location location, void *user_data)
{
    YsParseError exc;
    exc.location = location;
    exc.msg.assign(msg, msg_len);
    throw exc;
}
} // anon namespace

RYML_EXPORT ysparse *ysparse_init()
{
    TIMED_SECTION("cpp:ysparse_init");
    Callbacks cb = {};
    cb.m_error = &ysparse_error;
    set_callbacks(cb);
    ysparse *ryml2evt = _RYML_CB_ALLOC(get_callbacks(), ysparse, 1);
    _RYML_CB_CHECK(get_callbacks(), ryml2evt != nullptr);
    new ((void*)ryml2evt) ysparse();
    return ryml2evt;
}

RYML_EXPORT void ysparse_destroy(ysparse *obj)
{
    TIMED_SECTION("cpp:ysparse_destroy");
    obj->~ysparse();
    _RYML_CB_FREE(get_callbacks(), obj, ysparse, 1);
}

RYML_EXPORT bool ysparse_parse(ysparse *obj,
                               const char *filename,
                               char *ys, size_type ys_size,
                               char *arena, size_type arena_size,
                               int *events, size_type evt_size)
{
    TIMED_SECTION("cpp:ysparse", ys_size);
    csubstr filename_ = filename ? to_csubstr(filename) : csubstr{};
    substr ys_(ys, (size_t)ys_size);
    substr arena_(arena, (size_t)arena_size);
    {
        TIMED_SECTION("cpp:ysparse/reset");
        obj->reset(ys_, arena_, events, evt_size);
    }
    {
        TIMED_SECTION("cpp:ysparse/parse", ys_size);
        obj->m_parser.parse_in_place_ev(filename_, ys_);
    }
    return obj->m_handler.fits_buffers();
}

RYML_EXPORT int ysparse_reqsize_evt(ysparse *obj)
{
    return obj->m_handler.required_size_events();
}

RYML_EXPORT int ysparse_reqsize_arena(ysparse *obj)
{
    return (int)obj->m_handler.required_size_arena();
}

#if defined(__cplusplus)
}
#endif
