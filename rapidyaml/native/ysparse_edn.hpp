#pragma once
#ifndef YSPARSE_EDN_HPP_
#define YSPARSE_EDN_HPP_

#include "ysparse_edn_handler.hpp"
#include "ysparse_common.hpp"

#if defined(__cplusplus)
extern "C" {
#endif

struct RYML_EXPORT Ryml2Edn
{
    ys::EventHandlerEdn::EventSink m_sink;
    ys::EventHandlerEdn m_handler;
    c4::yml::ParseEngine<ys::EventHandlerEdn> m_parser;
    Ryml2Edn()
        : m_sink()
        , m_handler(&m_sink)
        , m_parser(&m_handler)
    {
    }
    void reset()
    {
        m_sink.reset();
        m_handler.reset();
    }
};


//-----------------------------------------------------------------------------

/** Initialize the resources */
RYML_EXPORT Ryml2Edn *ys2edn_init();

/** Destroy the resources */
RYML_EXPORT void ys2edn_destroy(Ryml2Edn *ryml2edn);

/** Parse YAML, and return corresponding EDN. Return the number of
 * characters needed for edn. The caller must check if the returned
 * size is larger than edn_size. If it is, call ys_retry_get() can be
 * called afterwards to extract the EDN. */
RYML_EXPORT size_type ys2edn_parse(Ryml2Edn *ryml2edn,
                                   const char *filename,
                                   char *ys, size_type ys_size,
                                   char *edn, size_type edn_size);

/** Get the edn from the previous call to ys2edn_parse(). */
RYML_EXPORT size_type ys2edn_retry_get(Ryml2Edn *ryml2edn,
                                       char *edn, size_type edn_size);

#if defined(__cplusplus)
}
#endif

#endif /* YSPARSE_EDN_HPP_ */
