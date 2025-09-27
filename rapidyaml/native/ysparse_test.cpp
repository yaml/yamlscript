#include <ysparse_evt.hpp>
#include <c4/std/string.hpp>
#include <c4/yml/extra/ints_utils.hpp>
#include <c4/format.hpp>
#include <vector>

using c4::csubstr;
using c4::substr;
using ryml::Location;

namespace ievt = c4::yml::extra::ievt;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

struct Ys2EvtScoped
{
    ysparse *ryml2evt;
    Ys2EvtScoped() : ryml2evt(ysparse_init()) {}
    ~Ys2EvtScoped() { if(ryml2evt) ysparse_destroy(ryml2evt); }
};


static bool showcmp = false;
struct TestResult
{
    uint32_t num_assertions;
    uint32_t num_tests;
    uint32_t num_failed_assertions;
    uint32_t num_failed_tests;
    operator bool() const { return num_failed_tests == 0 && num_failed_assertions == 0; }
    void add(TestResult const& that)
    {
        num_tests += 1 + that.num_tests;
        num_assertions += that.num_assertions;
        num_failed_tests += (that.num_failed_assertions > 0) + that.num_failed_tests;
        num_failed_assertions += that.num_failed_assertions;
    }
};

// provide a structured input for the events, grouping the relevant
// data in a single structure
struct EvtWithScalar
{
    int flags, str_start, str_len;
    csubstr scalar;
    bool needs_filter;
    EvtWithScalar(int t, int start=0, int len=0, csubstr sclr={}, bool needs_filter_=false)
    {
        flags = t;
        str_start = start;
        str_len = len;
        scalar = sclr;
        needs_filter = needs_filter_;
    }
    size_t required_size() const { return (flags & ievt::WSTR) ? 3u : 1u; }
};

size_t expected_size(std::vector<EvtWithScalar> const& evt)
{
    size_t exp = 0;
    for(EvtWithScalar const& e : evt)
        exp += e.required_size();
    return exp;
}

#define _runtest(name, ...)                               \
    do {                                                  \
        printf("[ RUN  ] %s ... \n", #name);              \
        TestResult tr_ = name(__VA_ARGS__);               \
        tr.add(tr_);                                      \
        printf("[ %s ] %s\n", tr_?"OK  ":"FAIL", #name);  \
        } while(0)

#define CHECK(cond)                                                 \
    do {                                                            \
        bool pass = !!(cond);                                       \
        ++tr.num_assertions;                                        \
        if(!pass) {                                                 \
            printf("%s:%d: fail! %s\n", __FILE__, __LINE__, #cond); \
            ++tr.num_failed_assertions;                             \
        }                                                           \
    } while(0)

#define CHECK_EQ(lhs, rhs)                                          \
    do {                                                            \
        bool pass = !!(lhs == rhs);                                 \
        ++tr.num_assertions;                                        \
        if(!pass) {                                                 \
            std::string slhs = c4::catrs<std::string>(lhs);         \
            std::string srhs = c4::catrs<std::string>(rhs);         \
            printf("%s:%d: fail! %s=%s == %s=%s\n", __FILE__, __LINE__, #lhs, slhs.c_str(), #rhs, srhs.c_str()); \
            ++tr.num_failed_assertions;                             \
        }                                                           \
    } while(0)

#define CHECK_MSG(cond, fmt, ...)                                       \
    do {                                                                \
        bool pass = !!(cond);                                           \
        ++tr.num_assertions;                                            \
        if(!pass) {                                                     \
            printf("%s:%d: fail! %s:" fmt "\n", __FILE__, __LINE__, #cond, ## __VA_ARGS__); \
            ++tr.num_failed_assertions;                                 \
        }                                                               \
    } while(0)


struct TestCase
{
    int line;
    csubstr ys;
    std::vector<EvtWithScalar> evt;

public:

    TestResult test(ysparse *ryml2evt) const
    {
        TestResult tr = {};
        _runtest(test_evt_large_enough, );
        _runtest(test_evt_too_small, );
        _runtest(test_evt_nullptr, );
        _runtest(test_evt_large_enough_reuse, ryml2evt);
        _runtest(test_evt_too_small_reuse, ryml2evt);
        _runtest(test_evt_nullptr_reuse, ryml2evt);
        return tr;
    }

    // happy path: large-enough destination string
    TestResult test_evt_large_enough_reuse(ysparse *ryml2evt) const
    {
        if(evt.empty()) return {};
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        std::string arena_;
        arena_.resize((ys.size() * size_t(3)) / size_t(2));
        substr input = c4::to_substr(input_);
        substr arena = c4::to_substr(arena_);
        std::vector<ievt::DataType> output;
        output.resize(2 * expected_size(evt));
        int estimated_size = c4::yml::extra::estimate_events_ints_size(input);
        bool fits_buffers = ysparse_parse(ryml2evt, "ysfilename",
                                          input.str, (size_type)input.len,
                                          arena.str, (size_type)arena.len,
                                          &output[0], (size_type)output.size());
        CHECK(fits_buffers);
        int reqsize_evt = ysparse_reqsize_evt(ryml2evt);
        CHECK_MSG((size_t)reqsize_evt == expected_size(evt), "%d vs %zu", reqsize_evt, expected_size(evt));
        CHECK(reqsize_evt != 0);
        CHECK(reqsize_evt <= estimated_size);
        output.resize(reqsize_evt);
        CHECK(testeq(output, input, arena));
        return tr;
    }
    TestResult test_evt_large_enough() const
    {
        Ys2EvtScoped lib;
        return test_evt_large_enough_reuse(lib.ryml2evt);
    }

    // less-happy path: destination string not large enough
    TestResult test_evt_too_small_reuse(ysparse *ryml2evt) const
    {
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        std::string arena_;
        arena_.resize(0);
        substr input = c4::to_substr(input_);
        substr arena = c4::to_substr(arena_);
        std::vector<ievt::DataType> output;
        int estimated_size = c4::yml::extra::estimate_events_ints_size(input);
        bool fits_buffers = ysparse_parse(ryml2evt, "ysfilename",
                                          input.str, (size_type)input.len,
                                          arena.str, (size_type)arena.len,
                                          output.data(), (size_type)output.size());
        CHECK(!fits_buffers);
        int reqsize_evt = ysparse_reqsize_evt(ryml2evt);
        int reqsize_arena = ysparse_reqsize_arena(ryml2evt);
        CHECK(reqsize_evt == expected_size(evt));
        CHECK(reqsize_evt != 0);
        CHECK(reqsize_evt <= estimated_size);
        output.resize(reqsize_evt);
        arena_.resize(reqsize_arena);
        input_.assign(ys.begin(), ys.end()); // FIXME
        input = c4::to_substr(input_);
        arena = c4::to_substr(arena_);
        bool fits_buffers2 = ysparse_parse(ryml2evt, "ysfilename",
                                           input.str, (size_type)input.len,
                                           arena.str, (size_type)arena.len,
                                           output.data(), (size_type)output.size());
        CHECK(fits_buffers2);
        int reqsize_evt2 = ysparse_reqsize_evt(ryml2evt);
        int reqsize_arena2 = ysparse_reqsize_arena(ryml2evt);
        CHECK(reqsize_evt2 == reqsize_evt);
        CHECK(reqsize_arena2 == reqsize_arena);
        output.resize(reqsize_evt2);
        CHECK(testeq(output, input, arena));
        return tr;
    }
    TestResult test_evt_too_small() const
    {
        Ys2EvtScoped lib;
        return test_evt_too_small_reuse(lib.ryml2evt);
    }

    // safe calling with nullptr
    TestResult test_evt_nullptr_reuse(ysparse *ryml2evt) const
    {
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        int estimated_size = c4::yml::extra::estimate_events_ints_size(input);
        bool fits_buffers = ysparse_parse(ryml2evt, "ysfilename",
                                          input.str, (size_type)input.len,
                                          nullptr, 0,
                                          nullptr, 0);
        CHECK(!fits_buffers);
        int reqsize_evt = ysparse_reqsize_evt(ryml2evt);
        int reqsize_arena = ysparse_reqsize_arena(ryml2evt);
        CHECK(reqsize_evt == expected_size(evt));
        CHECK(reqsize_evt <= estimated_size);
        CHECK(reqsize_evt != 0);
        std::string arena_;
        arena_.resize(reqsize_arena);
        substr arena = c4::to_substr(arena_);
        std::vector<ievt::DataType> output;
        output.resize(reqsize_evt);
        input_.assign(ys.begin(), ys.end()); // FIXME
        input = c4::to_substr(input_);
        bool fits_buffers2 = ysparse_parse(ryml2evt, "ysfilename",
                                           input.str, (size_type)input.len,
                                           arena.str, (size_type)arena.len,
                                           output.data(), (size_type)output.size());
        CHECK(fits_buffers2);
        int reqsize_evt2 = ysparse_reqsize_evt(ryml2evt);
        int reqsize_arena2 = ysparse_reqsize_arena(ryml2evt);
        CHECK(reqsize_evt2 == reqsize_evt);
        CHECK(reqsize_arena2 == reqsize_arena);
        CHECK(reqsize_evt2 == output.size());
        CHECK(testeq(output, input, arena));
        return tr;
    }
    TestResult test_evt_nullptr() const
    {
        Ys2EvtScoped lib;
        return test_evt_nullptr_reuse(lib.ryml2evt);
    }

public:

    bool testeq(std::vector<ievt::DataType> const& actual, csubstr parsed_source, csubstr arena) const
    {
        int status = true;
        size_t num_events_expected = evt.size();
        size_t num_ints_expected = expected_size(evt);
        bool same_size = true;
        if(actual.size() != num_ints_expected)
        {
            printf("------\n"
                   "FAIL: different size\n"
                   "input:~~~%.*s~~~\n"
                   "expected size:~~~%zu~~~\n"
                   "actual size:~~~%zu~~~\n",
                   (int)ys.len, ys.str,
                   num_ints_expected,
                   actual.size());
            same_size = false;
        }
        for(size_t ia = 0, ie = 0; ie < num_events_expected; ++ie)
        {
            if(ia >= actual.size())
            {
                printf("fail: bad actual size. i=%zu vs %zu=actual.size()=\n", ia, actual.size());
                status = false;
                break;
            }
            #define _testcmp(fmt, cmp, ...) \
                if(showcmp) { printf("status=%d cmp=%d ie=%zu ia=%zu: " fmt "\n", status, (cmp), ie, ia, ## __VA_ARGS__); } \
                status &= (cmp)
            char actualbuf_[100];
            char expectedbuf_[100];
            csubstr actualbuf = c4::yml::extra::ievt::to_chars_sub(actualbuf_, actual[ia] & ievt::MASK);
            csubstr expectedbuf = c4::yml::extra::ievt::to_chars_sub(expectedbuf_, evt[ie].flags & ievt::MASK);
            _testcmp("exp=%d(%.*s) vs act=%d(%.*s)", evt[ie].flags == actual[ia],
                     evt[ie].flags, (int)expectedbuf.len, expectedbuf.str,
                     actual[ia], (int)actualbuf.len, actualbuf.str);
            status &= (evt[ie].flags == actual[ia]);
            if((evt[ie].flags & ievt::WSTR) && (actual[ia] & ievt::WSTR))
            {
                csubstr region = (actual[ia] & ievt::AREN) ? arena : parsed_source;
                _testcmp("   exp=%d vs act=%d", evt[ie].str_start == actual[ia + 1], evt[ie].str_start, actual[ia + 1]);
                _testcmp("   exp=%d vs act=%d", evt[ie].str_len == actual[ia + 2], evt[ie].str_len, actual[ia + 2]);
                bool safeactual = (ia + 2 < actual.size()) && (actual[ia + 1] < (int)region.len && actual[ia + 1] + actual[ia + 2] <= (int)region.len);
                bool safeexpected = (evt[ie].str_start < (int)region.len && evt[ie].str_start + evt[ie].str_len <= (int)region.len);
                _testcmp("   safeactual=%d", safeactual, safeactual);
                _testcmp("   safeactual=%d safeexpected=%d", safeactual == safeexpected, safeactual, safeexpected);
                if(safeactual && safeexpected)
                {
                    csubstr evtstr = region.sub((size_t)evt[ie].str_start, (size_t)evt[ie].str_len);
                    csubstr actualstr = region.sub((size_t)actual[ia + 1], (size_t)actual[ia + 2]);
                    _testcmp("   ref=[%zu]~~~%.*s~~~ vs act=[%zu]~~~%.*s~~~",
                           evt[ie].scalar == actualstr,
                           evt[ie].scalar.len, (int)evt[ie].scalar.len, evt[ie].scalar.str,
                           actualstr.len, (int)actualstr.len, actualstr.str);
                    if( ! evt[ie].needs_filter)
                    {
                        _testcmp("   exp=[%zu]~~~%.*s~~~ vs act=[%zu]~~~%.*s~~~",
                                 evtstr == actualstr,
                                 evtstr.len, (int)evtstr.len, evtstr.str,
                                 actualstr.len, (int)actualstr.len, actualstr.str);
                    }
                }
            }
            ia += (actual[ia] & ievt::WSTR) ? 3 : 1;
        }
        if(!status)
            printf("------\n"
                   "FAIL:\n"
                   "input:~~~%.*s~~~\n",
                   (int)ys.len, ys.str);
        return status && same_size;
    }
};


//-----------------------------------------------------------------------------

struct TestCaseErr
{
    int line;
    csubstr ys;
    bool is_parse_err;
    ryml::Location loc;

    TestCaseErr(int line_, csubstr ys_) : line(line_), ys(ys_), is_parse_err(false), loc() {}
    TestCaseErr(int line_, csubstr ys_, ryml::Location loc_) : line(line_), ys(ys_), is_parse_err(true), loc(loc_) {}

    TestResult test(ysparse *ryml2evt) const
    {
        TestResult tr = {};
        _runtest(test_err, );
        _runtest(test_err_reuse, ryml2evt);
        return tr;
    }

    TestResult test_err_reuse(ysparse *ryml2evt) const
    {
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        bool gotit = false;
        try
        {
            size_type reqsize = ysparse_parse(ryml2evt, "ysfilename",
                                              input.str, (size_type)input.len,
                                              nullptr, 0,
                                              nullptr, 0);
        }
        catch(YsParseError const& exc)
        {
            if(is_parse_err)
            {
                gotit = true;
                CHECK_EQ(exc.location.name, "ysfilename");
                CHECK_EQ(exc.location.line, loc.line);
                CHECK_EQ(exc.location.col, loc.col);
                CHECK_EQ(exc.location.offset, loc.offset);
            }
        }
        catch(std::exception const& exc)
        {
            if(!is_parse_err)
                gotit = true;
        }
        CHECK(gotit);
        return tr;
        return tr;
    }
    TestResult test_err() const
    {
        Ys2EvtScoped lib;
        return test_err_reuse(lib.ryml2evt);
    }
};


//-----------------------------------------------------------------------------

namespace {
// make the declarations shorter
#define tc(ys, ...) {__LINE__, ys, std::vector<EvtWithScalar>(__VA_ARGS__)}
#define e(...) EvtWithScalar{__VA_ARGS__}
using namespace ievt;
inline constexpr bool needs_filter = true;
const TestCase test_cases[] = {
    // case -------------------------------------------------
    tc("!yamlscript/v0/bare\n--- !code\n42\n",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|TAG_, 0, 19, "!yamlscript/v0/bare"),
           e(VAL_|SCLR|PLAI|PSTR, 0, 0, ""),
           e(EDOC|PSTR),
           e(BDOC|EXPL),
           e(VAL_|TAG_, 24, 5, "!code"),
           e(VAL_|SCLR|PLAI|PSTR, 30, 2, "42"),
           e(EDOC|PSTR),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc("a: 1",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI,      0, 1, "a"),
           e(VAL_|SCLR|PLAI|PSTR, 3, 1, "1"),
           e(EMAP|PSTR),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc("say: 2 + 2",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI,      0, 3, "say"),
           e(VAL_|SCLR|PLAI|PSTR, 5, 5, "2 + 2"),
           e(EMAP|PSTR),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc("𝄞: ✅",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI,      0, 4, "𝄞"),
           e(VAL_|SCLR|PLAI|PSTR, 6, 3, "✅"),
           e(EMAP|PSTR),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc("[a, b, c]",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BSEQ|FLOW),
           e(VAL_|SCLR|PLAI,      1, 1, "a"),
           e(VAL_|SCLR|PLAI|PSTR, 4, 1, "b"),
           e(VAL_|SCLR|PLAI|PSTR, 7, 1, "c"),
           e(ESEQ|PSTR),
           e(EDOC),
           e(ESTR),
       }),
    // case ------------------------------
    tc("[a: b]",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BSEQ|FLOW),
           e(VAL_|BMAP|FLOW),
           e(KEY_|SCLR|PLAI,      1, 1, "a"),
           e(VAL_|SCLR|PLAI|PSTR, 4, 1, "b"),
           e(EMAP|PSTR),
           e(ESEQ),
           e(EDOC),
           e(ESTR),
       }),
    // case ------------------------------
    tc(R"(--- !yamlscript/v0
foo: !
- {x: y}
- [x, y]
- foo
- 'foo'
- "foo"
- |
  foo
- >
  foo
- [1, 2, true, false, null]
- &anchor-1 !tag-1 foobar
---
another: doc
)",
       {
           e(BSTR),
           e(BDOC|EXPL),
           e(VAL_|TAG_, 4, 14, "!yamlscript/v0"),
           e(VAL_|BMAP|BLCK|PSTR),
           e(KEY_|SCLR|PLAI, 19, 3, "foo"),
           e(VAL_|TAG_|PSTR, 24, 1, "!"),
           e(VAL_|BSEQ|BLCK|PSTR),
           e(VAL_|BMAP|FLOW),
           e(KEY_|SCLR|PLAI, 29, 1, "x"),
           e(VAL_|SCLR|PLAI|PSTR, 32, 1, "y"),
           e(EMAP|PSTR),
           e(VAL_|BSEQ|FLOW),
           e(VAL_|SCLR|PLAI, 38, 1, "x"),
           e(VAL_|SCLR|PLAI|PSTR, 41, 1, "y"),
           e(ESEQ|PSTR),
           e(VAL_|SCLR|PLAI, 46, 3, "foo"),
           e(VAL_|SCLR|SQUO|PSTR, 53, 3, "foo"),
           e(VAL_|SCLR|DQUO|PSTR, 61, 3, "foo"),
           e(VAL_|SCLR|LITL|PSTR, 70, 4, "foo\n", needs_filter),
           e(VAL_|SCLR|FOLD|PSTR, 80, 4, "foo\n", needs_filter),
           e(VAL_|BSEQ|FLOW|PSTR),
           e(VAL_|SCLR|PLAI, 89, 1, "1"),
           e(VAL_|SCLR|PLAI|PSTR, 92, 1, "2"),
           e(VAL_|SCLR|PLAI|PSTR, 95, 4, "true"),
           e(VAL_|SCLR|PLAI|PSTR, 101, 5, "false"),
           e(VAL_|SCLR|PLAI|PSTR, 108, 4, "null"),
           e(ESEQ|PSTR),
           e(VAL_|TAG_, 126, 6, "!tag-1"),
           e(VAL_|ANCH|PSTR, 117, 8, "anchor-1"),
           e(VAL_|SCLR|PLAI|PSTR, 133, 6, "foobar"),
           e(ESEQ|PSTR),
           e(EMAP),
           e(EDOC),
           e(BDOC|EXPL),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 144, 7, "another"),
           e(VAL_|SCLR|PLAI|PSTR, 153, 3, "doc"),
           e(EMAP|PSTR),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc(R"(plain: well
  a
  b
  c
squo: 'single''quote'
dquo: "x\t\ny"
lit: |
     X
     Y
     Z
fold: >
     U
     V
     W
)",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 0, 5, "plain"),
           e(VAL_|SCLR|PLAI|PSTR, 7, 10, "well a b c"),
           e(KEY_|SCLR|PLAI|PSTR, 24, 4, "squo"),
           e(VAL_|SCLR|SQUO|PSTR, 31, 12, "single'quote", needs_filter),
           e(KEY_|SCLR|PLAI|PSTR, 46, 4, "dquo"),
           e(VAL_|SCLR|DQUO|PSTR, 53, 4, "x\t\ny", needs_filter),
           e(KEY_|SCLR|PLAI|PSTR, 61, 3, "lit"),
           e(VAL_|SCLR|LITL|PSTR, 68, 6, "X\nY\nZ\n", needs_filter),
           e(KEY_|SCLR|PLAI|PSTR, 89, 4, "fold"),
           e(VAL_|SCLR|FOLD|PSTR, 97, 6, "U V W\n", needs_filter),
           e(EMAP|PSTR),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc("- !!seq []",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BSEQ|BLCK),
           e(VAL_|TAG_, 2, 5, "!!seq"),
           e(VAL_|BSEQ|FLOW|PSTR),
           e(ESEQ),
           e(ESEQ),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc(R"_(defn run(prompt session=nil):
  when session:
    write session _ :append true: |+
      Q: $(orig-prompt:trim)
      A ($api-model):
      $(answer:trim)
)_",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI,       0, 28, "defn run(prompt session=nil)"),
           e(VAL_|BMAP|BLCK|PSTR),
           e(KEY_|SCLR|PLAI,      32, 12, "when session"),
           e(VAL_|BMAP|BLCK|PSTR),
           e(KEY_|SCLR|PLAI,      50, 28, "write session _ :append true"),
           e(VAL_|SCLR|LITL|PSTR, 83, 54, "Q: $(orig-prompt:trim)\nA ($api-model):\n$(answer:trim)\n", needs_filter),
           e(EMAP|PSTR),
           e(EMAP),
           e(EMAP),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc(R"_(#!/usr/bin/env ys-0

defn run(prompt session=nil):
  session-text =:
    when session && session:fs-e:

  answer =:
    cond:
      api-model =~ /^dall-e/:
        openai-image(prompt).data.0.url
      api-model.in?(anthropic-models):
        anthropic(prompt):anthropic-message:format
      api-model.in?(groq-models):
        groq(prompt).choices.0.message.content:format
      api-model.in?(openai-models):
        openai-chat(prompt).choices.0.message.content:format
      else: die()

  say: answer

  when session:
    write session _ :append true: |+
      Q: $(orig-prompt:trim)
      A ($api-model):
      $(answer:trim)

)_",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 21, 28, "defn run(prompt session=nil)"),
           e(VAL_|BMAP|BLCK|PSTR),
           e(KEY_|SCLR|PLAI, 53, 14, "session-text ="),
           e(VAL_|BMAP|BLCK|PSTR),
           e(KEY_|SCLR|PLAI, 73, 28, "when session && session:fs-e"),
           e(VAL_|SCLR|PLAI|PSTR, 0, 0, ""), // note empty scalar pointing at the front
           e(EMAP|PSTR),
           e(KEY_|SCLR|PLAI, 106, 8, "answer ="),
           e(VAL_|BMAP|BLCK|PSTR),
           e(KEY_|SCLR|PLAI, 120, 4, "cond"),
           e(VAL_|BMAP|BLCK|PSTR),
           e(KEY_|SCLR|PLAI, 132, 22, "api-model =~ /^dall-e/"),
           e(VAL_|SCLR|PLAI|PSTR, 164, 31, "openai-image(prompt).data.0.url"),
           e(KEY_|SCLR|PLAI|PSTR, 202, 31, "api-model.in?(anthropic-models)"),
           e(VAL_|SCLR|PLAI|PSTR, 243, 42, "anthropic(prompt):anthropic-message:format"),
           e(KEY_|SCLR|PLAI|PSTR, 292, 26, "api-model.in?(groq-models)"),
           e(VAL_|SCLR|PLAI|PSTR, 328, 45, "groq(prompt).choices.0.message.content:format"),
           e(KEY_|SCLR|PLAI|PSTR, 380, 28, "api-model.in?(openai-models)"),
           e(VAL_|SCLR|PLAI|PSTR, 418, 52, "openai-chat(prompt).choices.0.message.content:format"),
           e(KEY_|SCLR|PLAI|PSTR, 477, 4, "else"),
           e(VAL_|SCLR|PLAI|PSTR, 483, 5, "die()"),
           e(EMAP|PSTR),
           e(EMAP),
           e(KEY_|SCLR|PLAI, 492, 3, "say"),
           e(VAL_|SCLR|PLAI|PSTR, 497, 6, "answer"),
           e(KEY_|SCLR|PLAI|PSTR, 507, 12, "when session"),
           e(VAL_|BMAP|BLCK|PSTR),
           e(KEY_|SCLR|PLAI, 525, 28, "write session _ :append true"),
           e(VAL_|SCLR|LITL|PSTR, 558, 55, "Q: $(orig-prompt:trim)\nA ($api-model):\n$(answer:trim)\n\n", needs_filter),
           e(EMAP|PSTR),
           e(EMAP),
           e(EMAP),
           e(EDOC),
           e(ESTR),
       }),
};
#define tcf(...) TestCaseErr(__LINE__, __VA_ARGS__)
const TestCaseErr test_cases_err[] = {
    tcf("- !!str, xxx\n", Location(13, 2, 1)),
    //FIXME tcf(": : : :", Location(2, 1, 3)),
};
} // namespace


//-----------------------------------------------------------------------------

int main(int argc, const char *argv[])
{
    for(int i = 1; i < argc; ++i)
    {
        csubstr arg = ryml::to_csubstr(argv[i]);
        if(arg == "--timing" || arg == "-t")
            ysparse_timing_set(true);
    }
    Ys2EvtScoped ys2evt;
    TestResult total = {};
    size_t failed_cases = {};
    size_t num_cases = C4_COUNTOF(test_cases);
    for(size_t i = 0; i < C4_COUNTOF(test_cases); ++i)
    {
        printf("-----------------------------------------\n"
               "%s:%d: case %zu/%zu ...\n"
               "[%zu]~~~%.*s~~~\n",
               __FILE__, test_cases[i].line,
               i, num_cases, test_cases[i].ys.len, (int)test_cases[i].ys.len, test_cases[i].ys.str);
        const TestResult tr = test_cases[i].test(ys2evt.ryml2evt);
        total.add(tr);
        failed_cases += (!tr);
        printf("%s:%d: case %zu/%zu: %s\n",
               __FILE__, test_cases[i].line,
               i, C4_COUNTOF(test_cases), tr ? "ok!" : "failed");
    }
    size_t num_cases_err = C4_COUNTOF(test_cases_err);
    for(size_t i = 0; i < C4_COUNTOF(test_cases_err); ++i)
    {
        printf("-----------------------------------------\n"
               "%s:%d: errcase %zu/%zu ...\n"
               "[%zu]~~~%.*s~~~\n",
               __FILE__, test_cases[i].line,
               i, num_cases_err, test_cases_err[i].ys.len, (int)test_cases_err[i].ys.len, test_cases_err[i].ys.str);
        const TestResult tr = test_cases_err[i].test(ys2evt.ryml2evt);
        total.add(tr);
        failed_cases += (!tr);
        printf("%s:%d: case %zu/%zu: %s\n",
               __FILE__, test_cases[i].line,
               i, C4_COUNTOF(test_cases), tr ? "ok!" : "failed");
    }
    printf("assertions: %u/%u pass %u/%u fail\n", total.num_assertions - total.num_failed_assertions, total.num_assertions, total.num_failed_assertions, total.num_assertions);
    printf("tests: %u/%u pass %u/%u fail\n", total.num_tests - total.num_failed_tests, total.num_tests, total.num_failed_tests, total.num_tests);
    printf("cases: %zu/%zu pass %zu/%zu fail\n", num_cases-failed_cases, num_cases+num_cases_err, failed_cases, num_cases);
    if(total)
        printf("TESTS SUCCEED!\n");
    return total ? 0 : -1;
}
