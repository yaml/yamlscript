#ifndef _YSPARSE_EVT_HANDLER_HPP_
#define _YSPARSE_EVT_HANDLER_HPP_

#include <c4/yml/node_type.hpp>
#include <c4/yml/parse_engine.hpp>
#include <c4/yml/event_handler_stack.hpp>
#include <c4/yml/tag.hpp>
#include <c4/yml/std/string.hpp>
#include <c4/yml/detail/parser_dbg.hpp>

C4_SUPPRESS_WARNING_GCC_CLANG_PUSH
C4_SUPPRESS_WARNING_GCC_CLANG("-Wold-style-cast")
C4_SUPPRESS_WARNING_GCC("-Wuseless-cast")

namespace evt {
using DataType = int32_t;
typedef enum : DataType {
    // ---------------------
    // structure flags
    KEY_ = 1 <<  0,   // as key
    VAL_ = 1 <<  1,   // as value
    SCLR = 1 <<  2,   // =VAL
    BSEQ = 1 <<  3,   // +SEQ
    ESEQ = 1 <<  4,   // -SEQ
    BMAP = 1 <<  5,   // +MAP
    EMAP = 1 <<  6,   // -MAP
    ALIA = 1 <<  7,   // ref
    ANCH = 1 <<  8,   // anchor
    TAG_ = 1 <<  9,   // tag
    // ---------------------
    // style flags
    PLAI = 1 << 10,   // : (plain scalar)
    SQUO = 1 << 11,   // ' (single-quoted scalar)
    DQUO = 1 << 12,   // " (double-quoted scalar)
    LITL = 1 << 13,   // | (block literal scalar)
    FOLD = 1 << 14,   // > (block folded scalar)
    FLOW = 1 << 15,   // flow container: [] for seqs or {} for maps
    BLCK = 1 << 16,   // block container
    // ---------------------
    // document flags
    BDOC = 1 << 17,   // +DOC
    EDOC = 1 << 18,   // -DOC
    EXPL = 1 << 21,   // --- (with BDOC) or ... (with EDOC) (may be fused with FLOW if needed)
    BSTR = 1 << 19,   // +STR
    ESTR = 1 << 20,   // -STR
    // ---------------------
    // utility flags
    LAST = EXPL,
    MASK = (LAST << 1) - 1,
    HAS_STR = SCLR|ALIA|ANCH|TAG_ // the event requires a string. the next two integers will provide respectively the string's offset and length
} EventFlags;
} // namespace evt


namespace ys {

using c4::csubstr;
using c4::substr;
using c4::to_substr;
using c4::to_csubstr;
#ifdef RYML_DBG
using c4::_dbg_printf;
#endif

struct EventHandlerEvtState : public c4::yml::ParserState
{
    c4::yml::type_bits evt_type;
    int32_t evt_id;
};


struct EventHandlerEvt : public c4::yml::EventHandlerStack<EventHandlerEvt, EventHandlerEvtState>
{

    /** @name types
     * @{ */

    // our internal state must inherit from parser state
    using state = EventHandlerEvtState;

    /** @} */

public:

    /** @cond dev */
    csubstr m_str;
    evt::DataType * m_evt;
    int32_t m_evt_curr;
    int32_t m_evt_prev;
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

    EventHandlerEvt(c4::yml::Callbacks const& cb)
        : EventHandlerStack(cb)
    {
        reset({}, nullptr, 0);
    }
    EventHandlerEvt()
        : EventHandlerEvt(c4::yml::get_callbacks())
    {
    }

    void reset(csubstr str, evt::DataType *dst, int32_t dst_size)
    {
        _stack_reset_root();
        m_curr->flags |= c4::yml::RUNK|c4::yml::RTOP;
        m_curr->evt_type = {};
        m_curr->evt_id = 0;
        m_arena.clear();
        m_str = str;
        m_evt = dst;
        m_evt_size = dst_size;
        m_evt_curr = 0;
        m_evt_prev = 0;
    }

    void reserve(int arena_size)
    {
        m_arena.reserve(arena_size);
    }

    /** @} */

public:

    /** @name parse events
     * @{ */

    void start_parse(const char* filename, c4::yml::detail::pfn_relocate_arena relocate_arena, void *relocate_arena_data)
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
        _c4dbgpf("{}/{}: begin_doc", m_evt_curr, m_evt_size);
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
        _c4dbgpf("{}/{}: end_doc", m_evt_curr, m_evt_size);
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
        _c4dbgpf("{}/{}: begin_doc_expl", m_evt_curr, m_evt_size);
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
        _c4dbgpf("{}/{}: end_doc_expl", m_evt_curr, m_evt_size);
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
        _c4dbgpf("{}/{}: bmap flow", m_evt_curr, m_evt_size);
        _send_flag_only_(evt::VAL_|evt::BMAP|evt::FLOW);
        _mark_parent_with_children_();
        _enable_(c4::yml::MAP|c4::yml::FLOW_SL);
        _push();
    }
    void begin_map_val_block()
    {
        _c4dbgpf("{}/{}: bmap block", m_evt_curr, m_evt_size);
        _send_flag_only_(evt::VAL_|evt::BMAP|evt::BLCK);
        _mark_parent_with_children_();
        _enable_(c4::yml::MAP|c4::yml::BLOCK);
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
        _c4dbgpf("{}/{}: bseq flow", m_evt_curr, m_evt_size);
        _send_flag_only_(evt::VAL_|evt::BSEQ|evt::FLOW);
        _mark_parent_with_children_();
        _enable_(c4::yml::SEQ|c4::yml::FLOW_SL);
        _push();
    }
    void begin_seq_val_block()
    {
        _c4dbgpf("{}/{}: bseq block", m_evt_curr, m_evt_size);
        _send_flag_only_(evt::VAL_|evt::BSEQ|evt::BLCK);
        _mark_parent_with_children_();
        _enable_(c4::yml::SEQ|c4::yml::BLOCK);
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
        _RYML_CB_ASSERT(m_stack.m_callbacks, m_evt_curr > 2);
        _RYML_CB_ASSERT(m_stack.m_callbacks, m_evt_prev > 0);
        _c4dbgpf("{}/{}: prev={} actually_val_is_first_key_of_new_map_flow", m_evt_curr, m_evt_size, m_evt_prev);
        // BEFORE
        // ... flag start len (free)
        //     |              |
        //     prev           curr
        // AFTER
        // ... flag flag start len (free)
        //          |              |
        //          prev           curr
        if(m_evt_prev < m_evt_size)
        {
            _RYML_CB_ASSERT(m_stack.m_callbacks, (m_evt[m_evt_prev] & evt::HAS_STR) || m_evt_curr >= m_evt_size);
            if(m_evt_curr < m_evt_size)
            {
                // watchout: it must be in this order!
                m_evt[m_evt_curr    ] = m_evt[m_evt_prev + 2];
                m_evt[m_evt_curr - 1] = m_evt[m_evt_prev + 1];
                m_evt[m_evt_curr - 2] = m_evt[m_evt_prev] | evt::KEY_;
                m_evt[m_evt_curr - 2] &= ~evt::VAL_;
            }
            m_evt[m_evt_prev] = evt::BMAP|evt::FLOW|evt::VAL_;
        }
        m_curr->evt_id = m_evt_curr - 2;
        ++m_evt_prev;
        ++m_evt_curr;
        _enable_(c4::yml::MAP|c4::yml::FLOW);
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


    C4_ALWAYS_INLINE void set_key_scalar_plain_empty()
    {
        _c4dbgpf("{}/{}: set_key_scalar_plain_empty", m_evt_curr, m_evt_size);
        _send_key_scalar_(_get_latest_empty_scalar(), evt::PLAI);
        _enable_(c4::yml::KEY|c4::yml::KEY_PLAIN|c4::yml::KEYNIL);
    }
    C4_ALWAYS_INLINE void set_val_scalar_plain_empty()
    {
        _c4dbgpf("{}/{}: set_val_scalar_plain_empty", m_evt_curr, m_evt_size);
        _send_val_scalar_(_get_latest_empty_scalar(), evt::PLAI);
        _enable_(c4::yml::VAL|c4::yml::VAL_PLAIN|c4::yml::VALNIL);
    }
    C4_ALWAYS_INLINE csubstr _get_latest_empty_scalar() const
    {
        // ideally we should search back in the latest event that has
        // a scalar, than select a zero-length scalar immediately
        // after that scalar. But this also works for now:
        return m_str.first(0);
    }


    C4_ALWAYS_INLINE void set_key_scalar_plain(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_key_scalar_plain: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_key_scalar_(scalar, evt::PLAI);
        _enable_(c4::yml::KEY|c4::yml::KEY_PLAIN);
    }
    C4_ALWAYS_INLINE void set_val_scalar_plain(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_val_scalar_plain: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_val_scalar_(scalar, evt::PLAI);
        _enable_(c4::yml::VAL|c4::yml::VAL_PLAIN);
    }


    C4_ALWAYS_INLINE void set_key_scalar_dquoted(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_key_scalar_dquo: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_key_scalar_(scalar, evt::DQUO);
        _enable_(c4::yml::KEY|c4::yml::KEY_DQUO);
    }
    C4_ALWAYS_INLINE void set_val_scalar_dquoted(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_val_scalar_dquo: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_val_scalar_(scalar, evt::DQUO);
        _enable_(c4::yml::VAL|c4::yml::VAL_DQUO);
    }


    C4_ALWAYS_INLINE void set_key_scalar_squoted(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_key_scalar_squo: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_key_scalar_(scalar, evt::SQUO);
        _enable_(c4::yml::KEY|c4::yml::KEY_SQUO);
    }
    C4_ALWAYS_INLINE void set_val_scalar_squoted(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_val_scalar_squo: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_val_scalar_(scalar, evt::SQUO);
        _enable_(c4::yml::VAL|c4::yml::VAL_SQUO);
    }


    C4_ALWAYS_INLINE void set_key_scalar_literal(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_key_scalar_literal: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_key_scalar_(scalar, evt::LITL);
        _enable_(c4::yml::KEY|c4::yml::KEY_LITERAL);
    }
    C4_ALWAYS_INLINE void set_val_scalar_literal(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_val_scalar_literal: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_val_scalar_(scalar, evt::LITL);
        _enable_(c4::yml::VAL|c4::yml::VAL_LITERAL);
    }


    C4_ALWAYS_INLINE void set_key_scalar_folded(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_key_scalar_folded: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_key_scalar_(scalar, evt::FOLD);
        _enable_(c4::yml::KEY|c4::yml::KEY_FOLDED);
    }
    C4_ALWAYS_INLINE void set_val_scalar_folded(csubstr scalar)
    {
        _c4dbgpf("{}/{}: set_val_scalar_folded: @{} [{}]~~~{}~~~", m_evt_curr, m_evt_size, scalar.str-m_str.str, scalar.len, scalar);
        _send_val_scalar_(scalar, evt::FOLD);
        _enable_(c4::yml::VAL|c4::yml::VAL_FOLDED);
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

#define _add_scalar_(i, scalar)                                     \
    _c4dbgpf("{}/{}: scalar!", i, m_evt_size);                      \
    _RYML_CB_ASSERT(m_stack.m_callbacks, scalar.is_sub(m_str));     \
    _RYML_CB_ASSERT(m_stack.m_callbacks, m_evt[i] & evt::HAS_STR);  \
    _RYML_CB_ASSERT(m_stack.m_callbacks, i + 2 < m_evt_size);       \
    m_evt[i + 1] = (evt::DataType)(scalar.str - m_str.str);         \
    m_evt[i + 2] = (evt::DataType)scalar.len

    /** @name YAML anchor/reference events */
    /** @{ */

    void set_key_anchor(csubstr anchor)
    {
        _c4dbgpf("{}/{}: set_key_anchor", m_evt_curr, m_evt_size);
        _enable_(c4::yml::KEYANCH);
        if(m_evt_curr + 2 < m_evt_size)
        {
            m_evt[m_evt_curr] = evt::KEY_|evt::ANCH;
            _add_scalar_(m_evt_curr, anchor);
        }
        m_evt_prev = m_evt_curr;
        m_evt_curr += 3;
    }
    void set_val_anchor(csubstr anchor)
    {
        _c4dbgpf("{}/{}: set_val_anchor", m_evt_curr, m_evt_size);
        _enable_(c4::yml::VALANCH);
        if(m_evt_curr + 2 < m_evt_size)
        {
            m_evt[m_evt_curr] = evt::VAL_|evt::ANCH;
            _add_scalar_(m_evt_curr, anchor);
        }
        m_evt_prev = m_evt_curr;
        m_evt_curr += 3;
    }

    void set_key_ref(csubstr ref)
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, ref.begins_with('*'));
        _enable_(c4::yml::KEY|c4::yml::KEYREF);
        _send_str_(ref.sub(1), evt::KEY_|evt::ALIA); // skip the leading *
    }
    void set_val_ref(csubstr ref)
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, ref.begins_with('*'));
        _enable_(c4::yml::VAL|c4::yml::VALREF);
        _send_str_(ref.sub(1), evt::VAL_|evt::ALIA); // skip the leading *
    }

    /** @} */

public:

    /** @name YAML tag events */
    /** @{ */

    void set_key_tag(csubstr tag)
    {
        _enable_(c4::yml::KEYTAG);
        csubstr ttag = _transform_directive(tag, m_key_tag_buf);
        _RYML_CB_ASSERT(m_stack.m_callbacks, !ttag.empty());
        if(ttag.begins_with('!') && !ttag.begins_with("!!"))
            ttag = ttag.sub(1);
        if(m_evt_curr + 2 < m_evt_size)
        {
            m_evt[m_evt_curr] = evt::KEY_|evt::TAG_;
            _add_scalar_(m_evt_curr, ttag);
        }
        m_evt_prev = m_evt_curr;
        m_evt_curr += 3;
    }
    void set_val_tag(csubstr tag)
    {
        _enable_(c4::yml::VALTAG);
        csubstr ttag = _transform_directive(tag, m_val_tag_buf);
        _RYML_CB_ASSERT(m_stack.m_callbacks, !ttag.empty());
        if(ttag.begins_with('!') && !ttag.begins_with("!!"))
            ttag = ttag.sub(1);
        if(m_evt_curr + 2 < m_evt_size)
        {
            m_evt[m_evt_curr] = evt::VAL_|evt::TAG_;
            _add_scalar_(m_evt_curr, ttag);
        }
        m_evt_prev = m_evt_curr;
        m_evt_curr += 3;
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

    template<c4::yml::type_bits bits> C4_ALWAYS_INLINE void _enable__() noexcept
    {
        m_curr->evt_type |= bits;
    }
    template<c4::yml::type_bits bits> C4_ALWAYS_INLINE void _disable__() noexcept
    {
        m_curr->evt_type &= ~bits;
    }
    template<c4::yml::type_bits bits> C4_ALWAYS_INLINE bool _has_any__() const noexcept
    {
        return (m_curr->evt_type & bits) != c4::yml::type_bits(0);
    }

    void _mark_parent_with_children_()
    {
        if(m_parent)
            m_parent->has_children = true;
    }

    C4_ALWAYS_INLINE void _send_flag_only_(evt::DataType flags)
    {
        _c4dbgpf("{}/{}: flag only", m_evt_curr, m_evt_size);
        if(m_evt_curr < m_evt_size)
            m_evt[m_evt_curr] = flags;
        m_curr->evt_id = m_evt_curr;
        m_evt_prev = m_evt_curr;
        ++m_evt_curr;
    }

    C4_ALWAYS_INLINE void _send_key_scalar_(csubstr scalar, evt::DataType flags)
    {
        _c4dbgpf("{}/{}: key scalar", m_evt_curr, m_evt_size);
        if(m_evt_curr + 2 < m_evt_size)
        {
            m_evt[m_evt_curr] = evt::SCLR|evt::KEY_|flags;
            _add_scalar_(m_evt_curr, scalar);
        }
        m_curr->evt_id = m_evt_curr;
        m_evt_prev = m_evt_curr;
        m_evt_curr += 3;
    }

    C4_ALWAYS_INLINE void _send_val_scalar_(csubstr scalar, evt::DataType flags)
    {
        _c4dbgpf("{}/{}: val scalar", m_evt_curr, m_evt_size);
        if(m_evt_curr + 2 < m_evt_size)
        {
            m_evt[m_evt_curr] = evt::SCLR|evt::VAL_|flags;
            _add_scalar_(m_evt_curr, scalar);
        }
        m_curr->evt_id = m_evt_curr;
        m_evt_prev = m_evt_curr;
        m_evt_curr += 3;
    }

    C4_ALWAYS_INLINE void _send_str_(csubstr scalar, evt::DataType flags)
    {
        _c4dbgpf("{}/{}: send str", m_evt_curr, m_evt_size);
        if(m_evt_curr + 2 < m_evt_size)
        {
            m_evt[m_evt_curr] = flags;
            _add_scalar_(m_evt_curr, scalar);
        }
        m_curr->evt_id = m_evt_curr;
        m_evt_prev = m_evt_curr;
        m_evt_curr += 3;
    }

    csubstr _transform_directive(csubstr tag, substr output)
    {
        if(tag.begins_with("!!"))
        {
            return tag;
        }
        else if(tag.begins_with('!'))
        {
            if(c4::yml::is_custom_tag(tag))
            {
                _RYML_CB_ERR_(m_stack.m_callbacks, "tag not found", m_curr->pos);
            }
        }
        csubstr result = c4::yml::normalize_tag_long(tag, output);
        _RYML_CB_CHECK(m_stack.m_callbacks, result.len > 0);
        _RYML_CB_CHECK(m_stack.m_callbacks, result.str);
        return result;
    }
#undef _enable_
#undef _disable_
#undef _has_any_

};

} // namespace ys

C4_SUPPRESS_WARNING_GCC_POP

#endif /* _C4_YML_EVENT_HANDLER_EVT_HPP_ */
