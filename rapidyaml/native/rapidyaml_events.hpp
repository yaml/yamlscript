#pragma once
#ifndef RAPIDYAML_EVENTS_H
#define RAPIDYAML_EVENTS_H

#include <ryml_all.hpp>

namespace ryml {
using namespace c4;
using namespace c4::yml;
} // namespace ryml

#if defined(__cplusplus)
extern "C" {
#endif

RYML_EXPORT void ys2edn_destroy(char *events);
RYML_EXPORT char * ys2edn_create(char *src);

#if defined(__cplusplus)
}
#endif

#endif /* RAPIDYAML_EVENTS_H */
