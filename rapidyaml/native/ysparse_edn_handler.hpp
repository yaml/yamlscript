#ifndef _YSPARSE_EDN_HANDLER_HPP_
#define _YSPARSE_EDN_HANDLER_HPP_

#include <c4/yml/node_type.hpp>
#include <c4/yml/node.hpp>
#include <c4/yml/parse_engine.hpp>
#include <c4/yml/event_handler_stack.hpp>
#include <c4/yml/std/string.hpp>
#include <c4/yml/detail/parser_dbg.hpp>

#include <vector>

C4_SUPPRESS_WARNING_GCC_CLANG_PUSH
C4_SUPPRESS_WARNING_GCC_CLANG("-Wold-style-cast")
C4_SUPPRESS_WARNING_GCC("-Wuseless-cast")


namespace ys {

using c4::csubstr;
using c4::substr;
using c4::to_substr;
using c4::to_csubstr;
using c4::yml::id_type;
using c4::yml::NodeType_e;
using c4::yml::type_bits;
#ifdef RYML_DBG
using c4::_dbg_printf;
#endif

struct EventHandlerEdnState : public c4::yml::ParserState
{
    c4::yml::NodeData ev_data;
};


struct EventHandlerEdn : public c4::yml::EventHandlerStack<EventHandlerEdn, EventHandlerEdnState>
{

    /** @name types
     * @{ */

    // our internal state must inherit from parser state
    using state = EventHandlerEdnState;

    struct EventSink
    {
        std::string result;
        void reset() noexcept { result.clear(); }
        void append(csubstr s) noexcept { result.append(s.str, s.len); }
        void append(char c) noexcept { result += c; }
        void insert(csubstr s, size_t pos) noexcept { result.insert(pos, s.str, s.len); }
        void insert(char c, size_t pos) noexcept { result.insert(pos, 1, c); }
        csubstr get() const { return csubstr(&result[0], result.size()); }
        substr get() { return substr(&result[0], result.size()); }
        size_t find_last(csubstr s) const { return result.rfind(s.str, std::string::npos, s.len); }
        void append_escaped(csubstr val);
    };

    /** @} */

public:

    /** @cond dev */
    EventSink *C4_RESTRICT m_sink;
    std::vector<EventSink> m_val_buffers;
    char m_key_tag_buf[256];
    char m_val_tag_buf[256];
    std::string m_arena;
    bool m_first_doc;

    // undefined at the end
    #define _enable_(bits) _enable__<bits>()
    #define _disable_(bits) _disable__<bits>()
    #define _has_any_(bits) _has_any__<bits>()
    /** @endcond */

public:

    /** @name construction and resetting
     * @{ */

    EventHandlerEdn(EventSink *sink, c4::yml::Callbacks const& cb)
        : EventHandlerStack(cb), m_sink(sink), m_val_buffers(), m_first_doc()
    {
        reset();
    }
    EventHandlerEdn(EventSink *sink)
        : EventHandlerEdn(sink, c4::yml::get_callbacks())
    {
    }

    void reset()
    {
        _stack_reset_root();
        m_curr->flags |= c4::yml::RUNK|c4::yml::RTOP;
        m_val_buffers.resize((size_t)m_stack.size());
        m_arena.clear();
        m_first_doc = true;
    }

    void reserve(int edn_size, int arena_size)
    {
        if(m_val_buffers.empty())
            m_val_buffers.resize((size_t)m_stack.size());
        if(m_sink)
            m_sink->result.reserve(edn_size);
        for(size_t i = 0; i < m_val_buffers.size(); ++i)
        {
            int sz = edn_size / (int(1) << (uint32_t)i);
            sz = sz >= 128 ? sz : 128;
            m_val_buffers[i].result.reserve((size_t)sz);
        }
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
        _buf_flush_();
    }

    /** @} */

public:

    /** @name YAML stream events */
    /** @{ */

    void begin_stream()
    {
        _send_("(\n");
    }

    void end_stream()
    {
        _send_(")\n");
        _buf_flush_();
    }

    /** @} */

public:

    /** @name YAML document events */
    /** @{ */

    /** implicit doc start (without ---) */
    void begin_doc()
    {
        _c4dbgp("begin_doc");
        if(_stack_should_push_on_begin_doc())
        {
            _c4dbgp("push!");
            _push();
            _enable_(c4::yml::DOC);
        }
        m_first_doc = false;
    }
    /** implicit doc end (without ...) */
    void end_doc()
    {
        _c4dbgp("end_doc");
        _send_("{:+ \"-DOC\"}\n");
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
        if(_stack_should_push_on_begin_doc())
        {
            _c4dbgp("push!");
            _push();
        }
        if (m_first_doc)
            m_first_doc = false;
        else
            _send_("{:+ \"+DOC\"}\n");
        _enable_(c4::yml::DOC);
    }
    /** explicit doc end, with ... */
    void end_doc_expl()
    {
        _c4dbgp("end_doc_expl");
        _send_("{:+ \"-DOC\"}\n");
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
        _send_("{:+ \"+MAP\"");
        _send_val_props_();
        _send_(", :flow true}\n");
        _mark_parent_with_children_();
        _enable_(c4::yml::MAP|c4::yml::FLOW_SL);
        _push();
    }
    void begin_map_val_block()
    {
        _send_("{:+ \"+MAP\"");
        _send_val_props_();
        _send_("}\n");
        _mark_parent_with_children_();
        _enable_(c4::yml::MAP|c4::yml::BLOCK);
        _push();
    }

    void end_map()
    {
        _pop();
        _send_("{:+ \"-MAP\"}\n");
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
        _send_("{:+ \"+SEQ\"");
        _send_val_props_();
        _send_(", :flow true}\n");
        _mark_parent_with_children_();
        _enable_(c4::yml::SEQ|c4::yml::FLOW_SL);
        _push();
    }
    void begin_seq_val_block()
    {
        _send_("{:+ \"+SEQ\"");
        _send_val_props_();
        _send_("}\n");
        _mark_parent_with_children_();
        _enable_(c4::yml::SEQ|c4::yml::BLOCK);
        _push();
    }

    void end_seq()
    {
        _pop();
        _send_("{:+ \"-SEQ\"}\n");
    }

    /** @} */

public:

    /** @name YAML structure events */
    /** @{ */

    void add_sibling()
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, m_parent);
        _buf_flush_to_(m_curr->level, m_parent->level);
        m_curr->ev_data = {};
    }

    /** set the previous val as the first key of a new map, with flow style.
     *
     * See the documentation for @ref doc_event_handlers, which has
     * important notes about this event.
     */
    void actually_val_is_first_key_of_new_map_flow()
    {
        // ensure we have a temporary buffer to save the current val
        const id_type tmp = m_curr->level + id_type(2);
        _buf_ensure_(tmp + id_type(2));
        // save the current val to the temporary buffer
        _buf_flush_to_(m_curr->level, tmp);
        // create the map.
        // this will push a new level, and tmp is one further
        begin_map_val_flow();
        _RYML_CB_ASSERT(m_stack.m_callbacks, tmp != m_curr->level);
        // now move the saved val as the first key
        _buf_flush_to_(tmp, m_curr->level);
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
        _send_key_scalar_({}, ':');
        _enable_(c4::yml::KEY|c4::yml::KEY_PLAIN|c4::yml::KEYNIL);
    }
    C4_ALWAYS_INLINE void set_val_scalar_plain_empty()
    {
        _send_val_scalar_({}, ':');
        _enable_(c4::yml::VAL|c4::yml::VAL_PLAIN|c4::yml::VALNIL);
    }


    C4_ALWAYS_INLINE void set_key_scalar_plain(csubstr scalar)
    {
        _send_key_scalar_(scalar, '=');
        _enable_(c4::yml::KEY|c4::yml::KEY_PLAIN);
    }
    C4_ALWAYS_INLINE void set_val_scalar_plain(csubstr scalar)
    {
        _send_val_scalar_(scalar, '=');
        _enable_(c4::yml::VAL|c4::yml::VAL_PLAIN);
    }


    C4_ALWAYS_INLINE void set_key_scalar_dquoted(csubstr scalar)
    {
        _send_key_scalar_(scalar, '$');
        _enable_(c4::yml::KEY|c4::yml::KEY_DQUO);
    }
    C4_ALWAYS_INLINE void set_val_scalar_dquoted(csubstr scalar)
    {
        _send_val_scalar_(scalar, '$');
        _enable_(c4::yml::VAL|c4::yml::VAL_DQUO);
    }


    C4_ALWAYS_INLINE void set_key_scalar_squoted(csubstr scalar)
    {
        _send_key_scalar_(scalar, '\'');
        _enable_(c4::yml::KEY|c4::yml::KEY_SQUO);
    }
    C4_ALWAYS_INLINE void set_val_scalar_squoted(csubstr scalar)
    {
        _send_val_scalar_(scalar, '\'');
        _enable_(c4::yml::VAL|c4::yml::VAL_SQUO);
    }


    C4_ALWAYS_INLINE void set_key_scalar_literal(csubstr scalar)
    {
        _send_key_scalar_(scalar, '|');
        _enable_(c4::yml::KEY|c4::yml::KEY_LITERAL);
    }
    C4_ALWAYS_INLINE void set_val_scalar_literal(csubstr scalar)
    {
        _send_val_scalar_(scalar, '|');
        _enable_(c4::yml::VAL|c4::yml::VAL_LITERAL);
    }


    C4_ALWAYS_INLINE void set_key_scalar_folded(csubstr scalar)
    {
        _send_key_scalar_(scalar, '>');
        _enable_(c4::yml::KEY|c4::yml::KEY_FOLDED);
    }
    C4_ALWAYS_INLINE void set_val_scalar_folded(csubstr scalar)
    {
        _send_val_scalar_(scalar, '>');
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

    /** @name YAML anchor/reference events */
    /** @{ */

    void set_key_anchor(csubstr anchor)
    {
        _enable_(c4::yml::KEYANCH);
        m_curr->ev_data.m_key.anchor = anchor;
    }
    void set_val_anchor(csubstr anchor)
    {
        _enable_(c4::yml::VALANCH);
        m_curr->ev_data.m_val.anchor = anchor;
    }

    void set_key_ref(csubstr ref)
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, ref.begins_with('*'));
        _enable_(c4::yml::KEY|c4::yml::KEYREF);
        _send_("{:+ \"=ALI\" :* \"");
        _send_(ref.sub(1));
        _send_("\"}\n");
    }
    void set_val_ref(csubstr ref)
    {
        _enable_(c4::yml::VAL|c4::yml::VALREF);
        _send_("{:+ \"=ALI\" :* \"");
        _send_(ref.sub(1));
        _send_("\"}\n");
    }

    /** @} */

public:

    /** @name YAML tag events */
    /** @{ */

    void set_key_tag(csubstr tag)
    {
        _enable_(c4::yml::KEYTAG);
        m_curr->ev_data.m_key.tag = _transform_directive(tag, m_key_tag_buf);
    }
    void set_val_tag(csubstr tag)
    {
        _enable_(c4::yml::VALTAG);
        m_curr->ev_data.m_val.tag = _transform_directive(tag, m_val_tag_buf);
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
        _buf_ensure_(m_stack.size() + id_type(1));
        _buf_().reset();
        m_curr->ev_data = {};
    }

    /** end the current scope */
    void _pop()
    {
        _buf_flush_to_(m_curr->level, m_parent->level);
        _stack_pop();
    }

    template<type_bits bits> C4_ALWAYS_INLINE void _enable__() noexcept
    {
        m_curr->ev_data.m_type.type = static_cast<NodeType_e>(m_curr->ev_data.m_type.type | bits);
    }
    template<type_bits bits> C4_ALWAYS_INLINE void _disable__() noexcept
    {
        m_curr->ev_data.m_type.type = static_cast<NodeType_e>(m_curr->ev_data.m_type.type & (~bits));
    }
    template<type_bits bits> C4_ALWAYS_INLINE bool _has_any__() const noexcept
    {
        return (m_curr->ev_data.m_type.type & bits) != 0;
    }

    void _mark_parent_with_children_()
    {
        if(m_parent)
            m_parent->has_children = true;
    }

    EventSink& _buf_() noexcept
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, (size_t)m_curr->level < m_val_buffers.size());
        return m_val_buffers[(size_t)m_curr->level];
    }

    EventSink& _buf_(id_type level) noexcept
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, (size_t)level < m_val_buffers.size());
        return m_val_buffers[(size_t)level];
    }

    EventSink const& _buf_(id_type level) const noexcept
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, (size_t)level < m_val_buffers.size());
        return m_val_buffers[(size_t)level];
    }

    static void _buf_flush_to_(EventSink &C4_RESTRICT src, EventSink &C4_RESTRICT dst) noexcept
    {
        dst.append(src.get());
        src.reset();
    }

    void _buf_flush_to_(id_type level_src, id_type level_dst) noexcept
    {
        auto &src = _buf_(level_src);
        auto &dst = _buf_(level_dst);
        _buf_flush_to_(src, dst);
    }

    void _buf_flush_() noexcept
    {
        _buf_flush_to_(_buf_(), *m_sink);
    }

    void _buf_ensure_(id_type size_needed) noexcept
    {
        if((size_t)size_needed > m_val_buffers.size())
            m_val_buffers.resize((size_t)size_needed);
    }

    C4_ALWAYS_INLINE void _send_(csubstr s) noexcept { _buf_().append(s); }
    C4_ALWAYS_INLINE void _send_(char c) noexcept { _buf_().append(c); }

    void _send_key_scalar_(csubstr scalar, char scalar_type_code)
    {
        _send_("{:+ \"=VAL\"");
        _send_key_props_();
        _send_(", :");
        _send_(scalar_type_code);
        _send_(" \"");
        _buf_().append_escaped(scalar);
        _send_("\"}\n");
    }
    void _send_val_scalar_(csubstr scalar, char scalar_type_code)
    {
        _send_("{:+ \"=VAL\"");
        _send_val_props_();
        _send_(", :");
        _send_(scalar_type_code);
        _send_(" \"");
        _buf_().append_escaped(scalar);
        _send_("\"}\n");
    }

    void _send_key_props_()
    {
        if(_has_any_(c4::yml::KEYANCH|c4::yml::KEYREF))
        {
            _send_(", :& \"");
            _send_(m_curr->ev_data.m_key.anchor);
            _send_('\"');
        }
        if(_has_any_(c4::yml::KEYTAG))
        {
            _send_(", :! \"");
            _send_tag_(m_curr->ev_data.m_key.tag);
            _send_('\"');
        }
        m_curr->ev_data.m_key = {};
        _disable_(c4::yml::KEYANCH|c4::yml::KEYREF|c4::yml::KEYTAG);
    }
    void _send_val_props_()
    {
        if(_has_any_(c4::yml::VALANCH|c4::yml::VALREF))
        {
            _send_(", :& \"");
            _send_(m_curr->ev_data.m_val.anchor);
            _send_('\"');
        }
        if(m_curr->ev_data.m_type.type & c4::yml::VALTAG)
        {
            _send_(", :! \"");
            _send_tag_(m_curr->ev_data.m_val.tag);
            _send_('\"');
        }
        m_curr->ev_data.m_val = {};
        _disable_(c4::yml::VALANCH|c4::yml::VALREF|c4::yml::VALTAG);
    }
    void _send_tag_(csubstr tag)
    {
        _RYML_CB_ASSERT(m_stack.m_callbacks, !tag.empty());
        if(tag.begins_with('!'))
            tag = tag.sub(1);
        _send_(tag);
    }

    csubstr _transform_directive(csubstr tag, substr output)
    {
        if(tag.begins_with('!'))
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

#endif /* _YSPARSE_EDN_HANDLER_HPP_ */
