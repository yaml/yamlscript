#pragma once
#ifndef RAPIDYAML_EVT_H_
#define RAPIDYAML_EVT_H_

#include <stdexcept>
#include "rapidyaml_evt_handler.hpp"

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
    c4::yml::EventHandlerEvt m_handler;
    c4::yml::ParseEngine<c4::yml::EventHandlerEvt> m_parser;
    Ryml2Evt()
        : m_handler()
        , m_parser(&m_handler)
    {
        RYML_CHECK(m_parser.options().scalar_filtering());
    }
    void reset(c4::csubstr src, evt::ParseEvent *evt, int32_t evt_size)
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

/** Parse YAML, and return corresponding EVT. Return the size needed
 * for evt. Nothing is written beyond evt_size. The caller must check
 * if the returned size is larger than evt_size. If so, this means
 * that evt could not accomodate all events produced from ys. The
 * caller must then resize evt to at least the return value, and the
 * function must be called again with the resized evt. */
RYML_EXPORT size_type ys2evt_parse(Ryml2Evt *ryml2evt,
                                   const char *filename,
                                   char *ys, size_type ys_size,
                                   evt::ParseEvent *evt, size_type evt_size);

#if defined(__cplusplus)
}
#endif

#endif /* RAPIDYAML_EVT_H */
