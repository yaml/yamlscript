#define RYML_SINGLE_HDR_DEFINE_NOW
#include <ryml_all.hpp>
#include "rapidyaml_edn_handler.hpp"
#include <string.h>

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml


#if defined(__cplusplus)
extern "C" {
#endif

RYML_EXPORT void ys2edn_destroy(char *events)
{
    ryml::Callbacks const& cb = ryml::get_callbacks();
    cb.m_free(events, strlen(events), cb.m_user_data);
}

RYML_EXPORT char * ys2edn_create(char *src)
{
    const std::string events = ryml::emit_edn_from_source(ryml::to_substr(src));
    const size_t sz = events.size();
    ryml::Callbacks const& cb = ryml::get_callbacks();
    void *mem = cb.m_allocate(1 + sz, nullptr, cb.m_user_data);
    char *out = static_cast<char*>(mem);
    if(sz)
        memcpy(out, events.data(), sz);
    out[sz] = '\0';
    return out;
}

#if defined(__cplusplus)
}
#endif
