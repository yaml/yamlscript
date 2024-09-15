#pragma once
#ifndef YSPARSE_EVT_HPP_
#define YSPARSE_EVT_HPP_

#include <stdexcept>
#include "ysparse_evt_handler.hpp"

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml

#if defined(__cplusplus)
extern "C" {
#endif

using size_type = int;

struct RYML_EXPORT Ryml2Evt
{
    ys::EventHandlerEvt m_handler;
    c4::yml::ParseEngine<ys::EventHandlerEvt> m_parser;
    Ryml2Evt()
        : m_handler()
        , m_parser(&m_handler)
    {
        RYML_CHECK(m_parser.options().scalar_filtering());
    }
    void reset(c4::csubstr src, evt::DataType *evt, int32_t evt_size)
    {
        m_handler.reset(src, evt, evt_size);
    }
};

struct RYML_EXPORT Ryml2EvtParseError : public std::exception
{
    c4::yml::Location location;
    std::string msg;
    const char* what() const noexcept override { return msg.c_str(); }
};


//-----------------------------------------------------------------------------

/** Initialize the resources */
RYML_EXPORT Ryml2Evt *ys2evt_init();

/** Destroy the resources */
RYML_EXPORT void ys2evt_destroy(Ryml2Evt *ryml2evt);

/** Parse YAML in the string `ys` of size `ys_size`, and write the
 * result into the array of (integer) events `evt` of size
 * `evt_size`. Each event is encoded as a mask of evt::EventFlags
 * (note that it uses the integer evt::DataType as the underlying
 * type), and when an event has an associated string, it is followed
 * in the array by two extra values, which encode the offset and the
 * length of the string in the `ys` string. The `ys` string is mutated
 * during parsing.
 *
 * @return the size needed for `evt`. The caller must check if the
 * returned size is larger than `evt_size`. If so, this means that
 * `evt` could not accomodate all events produced from `ys`, and is
 * incomplete. The caller must then (1) resize `evt` to at least the
 * return value, (2) re-copy the original YS into `ys` and (3) call
 * again this function, passing in the resized `evt` and the fresh
 * copy in `ys`.
 *
 * @note nothing is written beyond `evt_size`. This means that when
 * `evt_size` is 0, then `evt` can be null. This function can be
 * safely called for any valid pair of `evt` and `evt_size`, and will
 * always return the same required size.
 *
 * For example, the YAML `say: 2 + 2` produces the following sequence of
 * 12 integers:
 *
 * ```c++
 * BSTR,
 * BDOC,
 * VAL|BMAP|BLCK,
 * KEY|SCLR|PLAI, 0, 3, // "say"
 * VAL|SCLR|PLAI, 5, 5, // "2 + 2"
 * EMAP,
 * EDOC,
 * ESTR,
 * ```
 *
 * Note that the scalar events, ie "say" and "2 + 2", are followed
 * each by two extra integers encoding the offset and length of the
 * scalar's string. These two extra integers are present whenever the
 * event has any of the bits `SCLR`, `ALIA`, `ANCH` or `TAG`. For ease
 * of use, there is a bitmask `HAS_STR`, which enables quick testing
 * by a simple `flags & HAS_STR`. Refer to evt::EventFlags for the
 * full list of flags and their meaning.
 *
 * Also, where a string requires filtering, the parser filters it
 * in-place in the input string, and the extra integers will pertain
 * to the resulting filtered string.
 */
RYML_EXPORT size_type ys2evt_parse(Ryml2Evt *ryml2evt,
                                   const char *filename,
                                   char *ys, size_type ys_size,
                                   evt::DataType *evt, size_type evt_size);

#if defined(__cplusplus)
}
#endif

#endif /* YSPARSE_EVT_HPP_ */
