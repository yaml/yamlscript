#pragma once
#ifndef YSPARSE_COMMON_HPP_
#define YSPARSE_COMMON_HPP_

#include <stdexcept>
#include <c4/yml/export.hpp>

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml

using size_type = int;

struct YsParseError : public std::exception
{
    ryml::Location location;
    std::string msg;
    const char* what() const noexcept override { return msg.c_str(); }
};


//-----------------------------------------------------------------------------
// timing

#ifdef __cplusplus
extern "C" {
#endif
RYML_EXPORT bool ysparse_timing_get();
RYML_EXPORT void ysparse_timing_set(bool yes);
#ifdef __cplusplus
}
#endif

#ifndef YSPARSE_TIMED
#define TIMED_SECTION(...)
#error
#else
#include <stdio.h>
#include <chrono>
#define TIMED_SECTION(...) timed_section C4_XCAT(ts, __LINE__)(__VA_ARGS__)
struct timed_section
{
    using myclock = std::chrono::steady_clock;
    const char* name;
    size_type len;
    myclock::time_point start;
    C4_NO_INLINE timed_section(const char* n, size_type len_=0)
    {
        if(ysparse_timing_get())
        {
            name = n;
            len = len_;
            start = myclock::now();
            //fprintf(stderr, "%10s  : %s...\n", " ", name);
        }
    }
    C4_NO_INLINE ~timed_section()
    {
        if(ysparse_timing_get())
        {
            const std::chrono::duration<float, std::milli> t = myclock::now() - start;
            fprintf(stderr, "%10.6fms: %s", t.count(), name);
            if(len)
                fprintf(stderr, "  %.3fMB/s", (float)len / t.count() * 1.e-3);
            fprintf(stderr, "\n");
        }
    }
};
#endif // YSPARSE_TIMED

#endif // YSPARSE_COMMON_HPP_
