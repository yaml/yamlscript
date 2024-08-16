#pragma once
#ifndef RAPIDYAML_EVENTS_H
#define RAPIDYAML_EVENTS_H

#include <rapidyaml_all.hpp>
#include "rapidyaml_edn_handler.hpp"
#include <jni.h>

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml

#if defined(__cplusplus)
extern "C" {
#endif

using size_type = int;

struct RYML_EXPORT Ryml2Edn
{
    c4::yml::EventHandlerEdn::EventSink m_sink;
    c4::yml::EventHandlerEdn m_handler;
    c4::yml::ParseEngine<c4::yml::EventHandlerEdn> m_parser;
    JNIEnv *m_env;
    Ryml2Edn(JNIEnv *env)
        : m_sink()
        , m_handler(&m_sink)
        , m_parser(&m_handler)
        , m_env(env)
    {
    }
    void reset()
    {
        m_sink.reset();
        m_handler.reset();
    }
};

RYML_EXPORT Ryml2Edn *ys2edn_init(JNIEnv *env);
RYML_EXPORT void ys2edn_destroy(Ryml2Edn *ryml2edn);

/** (1) return the number of characters needed for edn.
 * The caller must check if the returned size is not larger
 * than edn_size. If it is, call ys_retry_get(). */
RYML_EXPORT size_type ys2edn(Ryml2Edn *ryml2edn,
                             const char *filename,
                             char *ys, size_type ys_size,
                             char *edn, size_type edn_size);

RYML_EXPORT size_type ys2edn_retry_get(Ryml2Edn *ryml2edn,
                                       char *edn, size_type edn_size);

/** (2) like (1), but raise an error if the size is not enough. */
RYML_EXPORT size_type ys2edn_failsmall(Ryml2Edn *ryml2edn,
                                       const char *filename,
                                       char *ys, size_type ys_size,
                                       char *edn, size_type edn_size);

RYML_EXPORT char * ys2edn_alloc(Ryml2Edn *ryml2edn,
                                const char *filename,
                                char *ys, size_type ys_size);
RYML_EXPORT void ys2edn_free(char *edn);

#if defined(__cplusplus)
}
#endif

#endif /* RAPIDYAML_EVENTS_H */
