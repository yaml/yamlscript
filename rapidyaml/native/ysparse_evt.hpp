#pragma once
#ifndef YSPARSE_EVT_HPP_
#define YSPARSE_EVT_HPP_

#include "c4/yml/parse_engine.hpp"
#include "c4/yml/parse_engine.def.hpp"
#include "c4/yml/extra/event_handler_ints.hpp"
#include "ysparse_common.hpp"

#if defined(__cplusplus)
extern "C" {
#endif

struct RYML_EXPORT ysparse
{
    c4::yml::extra::EventHandlerInts m_handler;
    c4::yml::ParseEngine<c4::yml::extra::EventHandlerInts> m_parser;
    ysparse()
        : m_handler()
        , m_parser(&m_handler)
    {
        RYML_CHECK(m_parser.options().scalar_filtering());
    }
    void reset(c4::substr src, c4::substr arena, int32_t *evt, int32_t evt_size)
    {
        m_handler.reset(src, arena, evt, evt_size);
    }
};


//-----------------------------------------------------------------------------

/** Initialize the resources */
RYML_EXPORT ysparse *ysparse_init();

/** Destroy the resources */
RYML_EXPORT void ysparse_destroy(ysparse *ryml2evt);

/** Parse YAML in the string `ys` of size `ys_size`, and write the
 * result into the array of (integer) events `evt` of size
 * `evt_size`. Each event is encoded as a mask of evt::EventFlags
 * (note that it uses the integer evt::DataType as the underlying
 * type), and when an event has an associated string, it is followed
 * in the array by two extra values, which encode the offset and the
 * length of the string in the `ys` string. The `ys` string is mutated
 * during parsing.
 *
 * @return true if the `evt` and `arena` buffers were large enough to
 * accomodate the result The caller must check this value. When false,
 * it means that at least one of the buffers could not accomodate the
 * result. The caller must then (1) resize `evt` to at least
 * the return value, (2) re-copy the original YS into `ys` and (3)
 * call again this function, passing in the resized `evt` and the
 * fresh copy in `ys`.
 *
 * @note nothing is written beyond `evt_size` or `arena_size`. This
 * means that when `evt_size`/`arena_size` is 0, then `evt`/`arena`
 * can be null. This function can be safely called for any valid pair
 * of `evt`+`evt_size` and `arena`/`arena_size`, and the same required
 * size will always be reported. The same applies for
 * `arena`+`arena_size`.
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
RYML_EXPORT bool ysparse_parse(ysparse *ryml2evt,
                               const char *filename,
                               char *ys, size_type ys_size,
                               char *arena, size_type arena_size,
                               int *evt, size_type evt_size);

/** Get the required size for the event buffer, from the last parse call */
RYML_EXPORT int ysparse_reqsize_evt(ysparse *ryml2evt);

/** Get the required size for the arena buffer, from the last parse call */
RYML_EXPORT int ysparse_reqsize_arena(ysparse *ryml2evt);

#if defined(__cplusplus)
}
#endif

#endif /* YSPARSE_EVT_HPP_ */
