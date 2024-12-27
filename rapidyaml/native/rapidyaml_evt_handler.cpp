#ifndef RYML_SINGLE_HEADER
#include <c4/yml/node.hpp>
#include <c4/yml/std/string.hpp>
#include <c4/yml/parse_engine.def.hpp>
#endif
#include "./rapidyaml_evt_handler.hpp"


namespace c4 {
namespace yml {

// instantiate the template
template class ParseEngine<EventHandlerEvt>;

} // namespace yml
} // namespace c4
