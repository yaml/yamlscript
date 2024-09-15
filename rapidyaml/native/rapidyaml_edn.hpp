#pragma once
#ifndef RAPIDYAML_EVENTS_H
#define RAPIDYAML_EVENTS_H

#include <stdexcept>
#include <rapidyaml_all.hpp>
#include "rapidyaml_edn_handler.hpp"

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml

#ifndef YS2EDN_TIMED
#define TIMED_SECTION(name)
#else
#include <chrono>
#define TIMED_SECTION(name) timed_section C4_XCAT(ts, __LINE__)(name)
struct timed_section
{
    using myclock = std::chrono::steady_clock;
    using fmsecs = std::chrono::duration<double, std::milli>;
    ryml::csubstr name;
    myclock::time_point start;
    fmsecs since() const { return myclock::now() - start; }
    timed_section(ryml::csubstr n) : name(n), start(myclock::now()) {}
    ~timed_section()
    {
        fprintf(stderr, "%.6fms: %.*s\n", since().count(), (int)name.len, name.str);
    }
};
#endif


#if defined(__cplusplus)
extern "C" {
#endif

using size_type = int;

struct RYML_EXPORT Ryml2Edn
{
    c4::yml::EventHandlerEdn::EventSink m_sink;
    c4::yml::EventHandlerEdn m_handler;
    c4::yml::ParseEngine<c4::yml::EventHandlerEdn> m_parser;
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

struct RYML_EXPORT Ryml2EdnParseError : public std::exception
{
    c4::yml::Location location;
    std::string msg;
    const char* what() const noexcept override { return msg.c_str(); }
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

#endif /* RAPIDYAML_EVENTS_H */
