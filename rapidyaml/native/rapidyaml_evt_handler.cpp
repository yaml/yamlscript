#include "./rapidyaml_evt_handler.hpp"
#include <c4/yml/node.hpp>
#include <c4/yml/parse_engine.def.hpp>

namespace c4 {
namespace yml {

// instantiate the template
template class ParseEngine<EventHandlerEvt>;

} // namespace yml
} // namespace c4
