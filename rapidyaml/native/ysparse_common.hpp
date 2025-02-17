#pragma once
#ifndef YSPARSE_COMMON_HPP_
#define YSPARSE_COMMON_HPP_

#include <stdexcept>

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

#ifndef YSPARSE_TIMED
#define TIMED_SECTION(...)
#else
#include <chrono>
#define TIMED_SECTION(...) timed_section C4_XCAT(ts, __LINE__)(__VA_ARGS__)
struct timed_section
{
    using myclock = std::chrono::steady_clock;
    ryml::csubstr name;
    size_type len;
    myclock::time_point start;
    timed_section(ryml::csubstr n, size_type len_=0) : name(n), len(len_), start(myclock::now()) {}
    ~timed_section()
    {
        const std::chrono::duration<double, std::milli> t = myclock::now() - start;
        fprintf(stderr, "%.6fms: %.*s", t.count(), (int)name.len, name.str);
        if(len)
            fprintf(stderr, "  %.3fMB/s", (double)len / t.count() * 1.e-3);
        fprintf(stderr, "\n");
    }
};
#endif // YSPARSE_TIMED

#endif // YSPARSE_COMMON_HPP_
