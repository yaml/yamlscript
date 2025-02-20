#include "ysparse_evt.hpp"

using namespace ryml;

#if defined(__cplusplus)
extern "C" {
#endif
// see
// https://stackoverflow.com/questions/230689/best-way-to-throw-exceptions-in-jni-code
// https://stackoverflow.com/questions/4138168/what-happens-when-i-throw-a-c-exception-from-a-native-java-method

namespace {
C4_NORETURN void ys2evt_parse_error(const char* msg, size_t msg_len, Location location, void *user_data)
{
    YsParseError exc;
    exc.location = location;
    exc.msg.assign(msg, msg_len);
    throw exc;
}
} // anon namespace

RYML_EXPORT Ryml2Evt *ys2evt_init()
{
    TIMED_SECTION("ys2evt_init");
    Callbacks cb = {};
    cb.m_error = &ys2evt_parse_error;
    set_callbacks(cb);
    Ryml2Evt *ryml2evt = _RYML_CB_ALLOC(get_callbacks(), Ryml2Evt, 1);
    _RYML_CB_CHECK(get_callbacks(), ryml2evt != nullptr);
    new ((void*)ryml2evt) Ryml2Evt();
    return ryml2evt;
}

RYML_EXPORT void ys2evt_destroy(Ryml2Evt *ryml2evt)
{
    TIMED_SECTION("ys2evt_destroy");
    ryml2evt->~Ryml2Evt();
    _RYML_CB_FREE(get_callbacks(), ryml2evt, Ryml2Evt, 1);
}

RYML_EXPORT size_type ys2evt_parse(Ryml2Evt *ryml2evt,
                                   const char *filename,
                                   char *ys, size_type ys_size,
                                   evt::DataType *events, size_type evt_size)
{
    TIMED_SECTION("ys2evt_parse", ys_size);
    csubstr filename_ = filename ? to_csubstr(filename) : csubstr{};
    substr ys_(ys, (size_t)ys_size);
    {
        TIMED_SECTION("reset + reserve");
        ryml2evt->reset(ys_, events, evt_size);
        ryml2evt->m_handler.reserve(256u);
    }
    {
        TIMED_SECTION("parse_in_place", ys_size);
        ryml2evt->m_parser.parse_in_place_ev(filename_, ys_);
    }
    return (size_type)ryml2evt->m_handler.m_evt_curr;
}

#if defined(__cplusplus)
}
#endif
