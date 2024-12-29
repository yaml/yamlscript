#ifndef _C4_YML_EVENT_HANDLER_EVT_HPP_
#define _C4_YML_EVENT_HANDLER_EVT_HPP_

#ifdef RYML_SINGLE_HEADER
#include <rapidyaml_all.hpp>
#else
#ifndef _C4_YML_EVENT_HANDLER_STACK_HPP_
#include "c4/yml/event_handler_stack.hpp"
#endif
#ifndef _C4_YML_STD_STRING_HPP_
#include "c4/yml/std/string.hpp"
#endif
#ifndef _C4_YML_DETAIL_PRINT_HPP_
#include "c4/yml/detail/print.hpp"
#endif
#endif

C4_SUPPRESS_WARNING_GCC_CLANG_PUSH
C4_SUPPRESS_WARNING_GCC_CLANG("-Wold-style-cast")
C4_SUPPRESS_WARNING_GCC("-Wuseless-cast")

namespace evt {
using EventFlagsType = int32_t;
typedef enum : EventFlagsType {
    // ---------------------
    // scalar flags
    SCLR = 1 <<  0,   // ( 1) has a scalar
    PLAI = 1 <<  1,   // ( 2) : (plain scalar)
    SQUO = 1 <<  2,   // ( 4) ' (single-quoted scalar)
    DQUO = 1 <<  3,   // ( 8) " (double-quoted scalar)
    LITL = 1 <<  4,   // (16) | (block literal scalar)
    FOLD = 1 <<  5,   // (32) > (block folded scalar)
    // ---------------------
    // container flags
    BSEQ = 1 <<  6,   // (  64) +SEQ (Begin SEQ)
    ESEQ = 1 <<  7,   // ( 128) -SEQ (End   SEQ)
    BMAP = 1 <<  8,   // ( 256) +MAP (Begin MAP)
    EMAP = 1 <<  9,   // ( 512) -MAP (End   MAP)
    FLOW = 1 << 10,   // (1024) flow container: [] for seqs or {} for maps
    BLCK = 1 << 11,   // (2048) block container
    // ---------------------
    // structure flags
    KEY_ = 1 << 12,   // (4096)
    VAL_ = 1 << 13,   // (8192)
    // ---------------------
    // document flags
    BDOC = 1 << 14,   // ( 16384) +DOC
    EDOC = 1 << 15,   // ( 32678) -DOC
    BSTR = 1 << 16,   // ( 65536) +STR
    ESTR = 1 << 17,   // (131072) -STR
    EXPL = 1 << 18,   // (262144) --- (with BDOC) or ... (with EDOC) (may be fused with FLOW if needed)
    // ---------------------
    // other flags
    ALIA = 1 << 19,   // ( 524288) ref
    ANCH = 1 << 20,   // (1048576) anchor
    TAG_ = 1 << 21,   // (2097152) tag
    // utility
    LAST = TAG_,
    MASK = (LAST << 1) - 1,
    HAS_STR = SCLR|ALIA|ANCH|TAG_
} EventFlags;

struct ParseEvent
{
    EventFlagsType flags;
    int32_t str_start; // index where the string starts
    int32_t str_len;   // length of the string
};
} // namespace evt


namespace c4 {
namespace yml {


/** @addtogroup doc_event_handlers
 * @{ */


struct EventHandlerEvtState : public ParserState
{
    NodeType evt_type;
    int32_t evt_id;
};


struct EventHandlerEvt : public EventHandlerStack<EventHandlerEvt, EventHandlerEvtState>
{

    /** @name types
     * @{ */

    // our internal state must inherit from parser state
    using state = EventHandlerEvtState;

    /** @} */

public:

    /** @cond dev */
    csubstr m_str;
    evt::ParseEvent * m_evt;
    int32_t m_evt_count;
    int32_t m_evt_size;
    char m_key_tag_buf[256];
    char m_val_tag_buf[256];
    std::string m_arena;

    // undefined at the end
    #define _enable_(bits) _enable__<bits>()
    #define _disable_(bits) _disable__<bits>()
    #define _has_any_(bits) _has_any__<bits>()
    /** @endcond */

public:

    /** @name construction and resetting
     * @{ */

    EventHandlerEvt(Callbacks const& cb)
        : EventHandlerStack(cb)
    {
        reset({}, nullptr, 0);
    }
    EventHandlerEvt()
        : EventHandlerEvt(get_callbacks())
    {
    }

    void reset(csubstr str, evt::ParseEvent *dst, int32_t dst_size)
    {
        _stack_reset_root();
        m_curr->flags |= RUNK|RTOP;
        m_curr->evt_type = {};
        m_curr->evt_id = 0;
        m_arena.clear();
        m_str = str;
        m_evt = dst;
        m_evt_size = dst_size;
        m_evt_count = 0;
    }

    void reserve(int arena_size)
    {
        m_arena.reserve(arena_size);
    }

    /** @} */

public:

    /** @name parse events
     * @{ */

    void start_parse(const char* filename, detail::pfn_relocate_arena relocate_arena, void *relocate_arena_data)
    {
        this->_stack_start_parse(filename, relocate_arena, relocate_arena_data);
    }

    void finish_parse()
    {
        this->_stack_finish_parse();
    }

    void cancel_parse()
    {
        while(m_stack.size() > 1)
            _pop();
    }

    /** @} */

public:

    /** @name YAML stream events */
    /** @{ */

    void begin_stream()
    {
        _send_flag_only_(evt::BSTR);
    }

    void end_stream()
    {
        _send_flag_only_(evt::ESTR);
    }

    /** @} */

public:

    /** @name YAML document events */
    /** @{ */

    /** implicit doc start (without ---) */
    void begin_doc()
    {
        _c4dbgp("begin_doc");
        _send_flag_only_(evt::BDOC);
        if(_stack_should_push_on_begin_doc())
        {
            _c4dbgp("push!");
            _push();
        }
    }
    /** implicit doc end (without ...) */
    void end_doc()
    {
        _c4dbgp("end_doc");
        _send_flag_only_(evt::EDOC);
        if(_stack_should_pop_on_end_doc())
        {
            _c4dbgp("pop!");
            _pop();
        }
    }

    /** explicit doc start, with --- */
    void begin_doc_expl()
    {
        _c4dbgp("begin_doc_expl");
        _send_flag_only_(evt::BDOC|evt::EXPL);
        if(_stack_should_push_on_begin_doc())
        {
            _c4dbgp("push!");
            _push();
        }
    }
    /** explicit doc end, with ... */
    void end_doc_expl()
    {
        _c4dbgp("end_doc_expl");
        _send_flag_only_(evt::EDOC|evt::EXPL);
        if(_stack_should_pop_on_end_doc())
        {
            _c4dbgp("pop!");
            _pop();
        }
    }

    /** @} */

public:

    /** @name YAML map functions */
    /** @{ */

    void begin_map_key_flow()
    {
        _RYML_CB_ERR(m_stack.m_callbacks, "container keys not supported");
    }
    void begin_map_key_block()
    {
        _RYML_CB_ERR(m_stack.m_callbacks, "container keys not supported");
    }

    void begin_map_val_flow()
    {
        _send_val_container_(evt::BMAP|evt::FLOW);
        _mark_parent_with_children_();
        _enable_(MAP|FLOW_SL);
        _push();
    }
    void begin_map_val_block()
    {
        _send_val_container_(evt::BMAP|evt::BLCK);
        _mark_parent_with_children_();
        _enable_(MAP|BLOCK);
        _push();
    }

    void end_map()
    {
        _pop();
        _send_flag_only_(evt::EMAP);
    }

    /** @} */

public:

    /** @name YAML seq events */
    /** @{ */

    void begin_seq_key_flow()
    {
        _RYML_CB_ERR(m_stack.m_callbacks, "container keys not supported");
    }
    void begin_seq_key_block()
    {
        _RYML_CB_ERR(m_stack.m_callbacks, "container keys not supported");
    }

    void begin_seq_val_flow()
    {
        _send_val_container_(evt::BSEQ|evt::FLOW);
        _mark_parent_with_children_();
        _enable_(SEQ|FLOW_SL);
        _push();
    }
    void begin_seq_val_block()
    {
        _send_val_container_(evt::BSEQ|evt::BLCK);
        _mark_parent_with_children_();
        _enable_(SEQ|BLOCK);
        _push();
    }

    void end_seq()
    {
        _pop();
        _send_flag_only_(evt::ESEQ);
    }

    /** @} */

public:

    /** @name YAML structure events */
    /** @{ */

    void add_sibling()
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, m_parent);
        m_curr->evt_type = {};
    }

    /** set the previous val as the first key of a new map, with flow style.
     *
     * See the documentation for @ref doc_event_handlers, which has
     * important notes about this event.
     */
    void actually_val_is_first_key_of_new_map_flow()
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, m_evt_count > 2);
        if(m_evt_count - 1 < m_evt_size)
        {
            evt::ParseEvent *C4_RESTRICT prev = &m_evt[m_evt_count - 1];
            _RYML_CB_ASSERT(m_stack.m_callbacks, (prev->flags & evt::HAS_STR));
            if(m_evt_count < m_evt_size)
            {
                evt::ParseEvent *C4_RESTRICT curr = &m_evt[m_evt_count];
                *curr = *prev;
                curr->flags &= ~evt::VAL_;
                curr->flags |= evt::KEY_;
            }
            prev->flags = evt::BMAP|evt::FLOW|evt::VAL_;
        }
        m_curr->evt_id = m_evt_count;
        ++m_evt_count;
        _enable_(MAP|FLOW);
        _push();
    }

    void actually_val_is_first_key_of_new_map_block()
    {
        _RYML_CB_ERR(m_stack.m_callbacks, "container keys not supported");
    }

    /** @} */

public:

    /** @name YAML scalar events */
    /** @{ */


    C4_ALWAYS_INLINE void set_key_scalar_plain(csubstr scalar)
    {
        _send_key_scalar_(scalar, evt::PLAI);
        _enable_(KEY|KEY_PLAIN);
    }
    C4_ALWAYS_INLINE void set_val_scalar_plain(csubstr scalar)
    {
        _send_val_scalar_(scalar, evt::PLAI);
        _enable_(VAL|VAL_PLAIN);
    }


    C4_ALWAYS_INLINE void set_key_scalar_dquoted(csubstr scalar)
    {
        _send_key_scalar_(scalar, evt::DQUO);
        _enable_(KEY|KEY_DQUO);
    }
    C4_ALWAYS_INLINE void set_val_scalar_dquoted(csubstr scalar)
    {
        _send_val_scalar_(scalar, evt::DQUO);
        _enable_(VAL|VAL_DQUO);
    }


    C4_ALWAYS_INLINE void set_key_scalar_squoted(csubstr scalar)
    {
        _send_key_scalar_(scalar, evt::SQUO);
        _enable_(KEY|KEY_SQUO);
    }
    C4_ALWAYS_INLINE void set_val_scalar_squoted(csubstr scalar)
    {
        _send_val_scalar_(scalar, evt::SQUO);
        _enable_(VAL|VAL_SQUO);
    }


    C4_ALWAYS_INLINE void set_key_scalar_literal(csubstr scalar)
    {
        _send_key_scalar_(scalar, evt::LITL);
        _enable_(KEY|KEY_LITERAL);
    }
    C4_ALWAYS_INLINE void set_val_scalar_literal(csubstr scalar)
    {
        _send_val_scalar_(scalar, evt::LITL);
        _enable_(VAL|VAL_LITERAL);
    }


    C4_ALWAYS_INLINE void set_key_scalar_folded(csubstr scalar)
    {
        _send_key_scalar_(scalar, evt::FOLD);
        _enable_(KEY|KEY_FOLDED);
    }
    C4_ALWAYS_INLINE void set_val_scalar_folded(csubstr scalar)
    {
        _send_val_scalar_(scalar, evt::FOLD);
        _enable_(VAL|VAL_FOLDED);
    }


    C4_ALWAYS_INLINE void mark_key_scalar_unfiltered()
    {
        _RYML_CB_ERR(m_stack.m_callbacks, "all scalars must be filtered");
    }
    C4_ALWAYS_INLINE void mark_val_scalar_unfiltered()
    {
        _RYML_CB_ERR(m_stack.m_callbacks, "all scalars must be filtered");
    }

    /** @} */

public:

#define index_to_str(dst, csubs)                                    \
    _RYML_CB_ASSERT(m_stack.m_callbacks, csubs.is_sub(m_str));      \
    dst##_start = (int32_t)(csubs.str - m_str.str);                 \
    dst##_len = (int32_t)csubs.len

    /** @name YAML anchor/reference events */
    /** @{ */

    void set_key_anchor(csubstr anchor)
    {
        _enable_(KEYANCH);
        if(m_evt_count < m_evt_size)
        {
            m_evt[m_evt_count].flags = evt::KEY_|evt::ANCH;
            index_to_str(m_evt[m_evt_count].str, anchor);
        }
        ++m_evt_count;
    }
    void set_val_anchor(csubstr anchor)
    {
        _enable_(VALANCH);
        if(m_evt_count < m_evt_size)
        {
            m_evt[m_evt_count].flags = evt::VAL_|evt::ANCH;
            index_to_str(m_evt[m_evt_count].str, anchor);
        }
        ++m_evt_count;
    }

    void set_key_ref(csubstr ref)
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, ref.begins_with('*'));
        _enable_(KEY|KEYREF);
        _send_key_scalar_(ref, evt::KEY_|evt::ALIA);
    }
    void set_val_ref(csubstr ref)
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, ref.begins_with('*'));
        _enable_(VAL|VALREF);
        _send_val_scalar_(ref, evt::VAL_|evt::ALIA);
    }

    /** @} */

public:

    /** @name YAML tag events */
    /** @{ */

    void set_key_tag(csubstr tag)
    {
        _enable_(KEYTAG);
        csubstr ttag = _transform_directive(tag, m_key_tag_buf);
        _RYML_CB_ASSERT(m_stack.m_callbacks, !ttag.empty());
        if(ttag.begins_with('!'))
            ttag = ttag.sub(1);
        if(m_evt_count < m_evt_size)
        {
            m_evt[m_evt_count].flags |= evt::KEY_|evt::TAG_;
            index_to_str(m_evt[m_evt_count].str, ttag);
        }
        ++m_evt_count;
    }
    void set_val_tag(csubstr tag)
    {
        _enable_(VALTAG);
        csubstr ttag = _transform_directive(tag, m_val_tag_buf);
        _RYML_CB_ASSERT(m_stack.m_callbacks, !ttag.empty());
        if(ttag.begins_with('!'))
            ttag = ttag.sub(1);
        if(m_evt_count < m_evt_size)
        {
            m_evt[m_evt_count].flags |= evt::VAL_|evt::TAG_;
            index_to_str(m_evt[m_evt_count].str, ttag);
        }
        ++m_evt_count;
    }

    /** @} */

public:

    /** @name YAML directive events */
    /** @{ */

    void add_directive(csubstr directive)
    {
        _RYML_CB_ERR(m_stack.m_callbacks, "tag directives not supported");
    }

    /** @} */

public:

    /** @name YAML arena events */
    /** @{ */

    substr alloc_arena(size_t len)
    {
        const size_t sz = m_arena.size();
        csubstr prev = to_csubstr(m_arena);
        m_arena.resize(sz + len);
        substr out = to_substr(m_arena).sub(sz);
        substr curr = to_substr(m_arena);
        if(curr.str != prev.str)
            _stack_relocate_to_new_arena(prev, curr);
        return out;
    }

    substr alloc_arena(size_t len, substr *relocated)
    {
        csubstr prev = to_csubstr(m_arena);
        if(!prev.is_super(*relocated))
            return alloc_arena(len);
        substr out = alloc_arena(len);
        substr curr = to_substr(m_arena);
        if(curr.str != prev.str)
            *relocated = _stack_relocate_to_new_arena(*relocated, prev, curr);
        return out;
    }

    /** @} */

public:

    /** @cond dev */

    /** push a new parent, add a child to the new parent, and set the
     * child as the current node */
    void _push()
    {
        _stack_push();
        m_curr->evt_type = {};
    }

    /** end the current scope */
    void _pop()
    {
        _stack_pop();
    }

    template<type_bits bits> C4_ALWAYS_INLINE void _enable__() noexcept
    {
        m_curr->evt_type.type = static_cast<NodeType_e>(m_curr->evt_type.type | bits);
    }
    template<type_bits bits> C4_ALWAYS_INLINE void _disable__() noexcept
    {
        m_curr->evt_type.type = static_cast<NodeType_e>(m_curr->evt_type.type & (~bits));
    }
    template<type_bits bits> C4_ALWAYS_INLINE bool _has_any__() const noexcept
    {
        return (m_curr->evt_type.type & bits) != 0;
    }

    void _mark_parent_with_children_()
    {
        if(m_parent)
            m_parent->has_children = true;
    }

    C4_ALWAYS_INLINE void _send_flag_only_(evt::EventFlagsType flags)
    {
        if(m_evt_count < m_evt_size)
        {
            m_evt[m_evt_count].flags = flags;
        }
        m_curr->evt_id = m_evt_count;
        ++m_evt_count;
    }

    C4_ALWAYS_INLINE void _send_val_container_(evt::EventFlagsType flags)
    {
        if(m_evt_count < m_evt_size)
        {
            m_evt[m_evt_count].flags = evt::VAL_|flags;
        }
        m_curr->evt_id = m_evt_count;
        ++m_evt_count;
        _disable_(VALANCH|VALREF|VALTAG); // maybe not needed?
    }

    C4_ALWAYS_INLINE void _send_key_scalar_(csubstr scalar, evt::EventFlagsType flags) noexcept
    {
        if(m_evt_count < m_evt_size)
        {
            m_evt[m_evt_count].flags = evt::SCLR|evt::KEY_|flags;
            index_to_str(m_evt[m_evt_count].str, scalar);
        }
        m_curr->evt_id = m_evt_count;
        ++m_evt_count;
        _disable_(KEYANCH|KEYTAG); // maybe not needed?
    }

    C4_ALWAYS_INLINE void _send_val_scalar_(csubstr scalar, evt::EventFlagsType flags) noexcept
    {
        if(m_evt_count < m_evt_size)
        {
            m_evt[m_evt_count].flags = evt::SCLR|evt::VAL_|flags;
            index_to_str(m_evt[m_evt_count].str, scalar);
        }
        m_curr->evt_id = m_evt_count;
        ++m_evt_count;
        _disable_(VALANCH|VALTAG); // maybe not needed?
    }

    csubstr _transform_directive(csubstr tag, substr output)
    {
        if(tag.begins_with('!'))
        {
            if(is_custom_tag(tag))
            {
                _RYML_CB_ERR_(m_stack.m_callbacks, "tag not found", m_curr->pos);
            }
        }
        csubstr result = normalize_tag_long(tag, output);
        _RYML_CB_CHECK(m_stack.m_callbacks, result.len > 0);
        _RYML_CB_CHECK(m_stack.m_callbacks, result.str);
        return result;
    }
#undef _enable_
#undef _disable_
#undef _has_any_

    /** @endcond */
};

/** @} */

} // namespace yml
} // namespace c4

C4_SUPPRESS_WARNING_GCC_POP

#endif /* _C4_YML_EVENT_HANDLER_EVT_HPP_ */
