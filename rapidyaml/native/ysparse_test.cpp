#include <ysparse_edn.hpp>
#include <ysparse_evt.hpp>
#include <c4/bitmask.hpp>

using c4::csubstr;
using c4::substr;

namespace c4
{
template<>
c4::EnumSymbols<evt::EventFlags> const esyms<evt::EventFlags>()
{
    static constexpr typename c4::EnumSymbols<evt::EventFlags>::Sym syms[] = {
        {evt::KEY_, "KEY_"},
        {evt::VAL_, "VAL_"},
        {evt::SCLR, "SCLR"},
        {evt::BSEQ, "BSEQ"},
        {evt::ESEQ, "ESEQ"},
        {evt::BMAP, "BMAP"},
        {evt::EMAP, "EMAP"},
        {evt::ALIA, "ALIA"},
        {evt::ANCH, "ANCH"},
        {evt::TAG_, "TAG_"},
        {evt::PLAI, "PLAI"},
        {evt::SQUO, "SQUO"},
        {evt::DQUO, "DQUO"},
        {evt::LITL, "LITL"},
        {evt::FOLD, "FOLD"},
        {evt::FLOW, "FLOW"},
        {evt::BLCK, "BLCK"},
        {evt::BDOC, "BDOC"},
        {evt::EDOC, "EDOC"},
        {evt::BSTR, "BSTR"},
        {evt::ESTR, "ESTR"},
        {evt::EXPL, "EXPL"},
    };
    return c4::EnumSymbols<evt::EventFlags>(syms);
}
}


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

struct Ys2EdnScoped
{
    Ryml2Edn *ryml2edn;
    Ys2EdnScoped() : ryml2edn(ys2edn_init()) {}
    ~Ys2EdnScoped() { if(ryml2edn) ys2edn_destroy(ryml2edn); }
};
struct Ys2EvtScoped
{
    Ryml2Evt *ryml2evt;
    Ys2EvtScoped() : ryml2evt(ys2evt_init()) {}
    ~Ys2EvtScoped() { if(ryml2evt) ys2evt_destroy(ryml2evt); }
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
    evt::DataType flags, str_start, str_len;
    csubstr scalar;
    bool needs_filter;
    EvtWithScalar(evt::DataType t, evt::DataType start=0, evt::DataType len=0, csubstr sclr={}, bool needs_filter_=false)
    {
        flags = t;
        str_start = start;
        str_len = len;
        scalar = sclr;
        needs_filter = needs_filter_;
    }
    size_t required_size() const { return (flags & evt::HAS_STR) ? 3u : 1u; }
};

size_t expected_size(std::vector<EvtWithScalar> const& evt)
{
    size_t exp = 0;
    for(EvtWithScalar const& e : evt)
        exp += e.required_size();
    return exp;
}

struct TestCase
{
    csubstr ys;
    csubstr edn;
    std::vector<EvtWithScalar> evt;

public:

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
    #define CHECK_MSG(cond, fmt, ...)                                   \
        do {                                                            \
            bool pass = !!(cond);                                       \
            ++tr.num_assertions;                                        \
            if(!pass) {                                                 \
                printf("%s:%d: fail! %s:" fmt "\n", __FILE__, __LINE__, #cond, ## __VA_ARGS__);   \
                ++tr.num_failed_assertions;                             \
            }                                                           \
        } while(0)

    TestResult test(Ryml2Edn *ryml2edn, Ryml2Evt *ryml2evt) const
    {
        TestResult tr = {};
        _runtest(test_edn_large_enough, );
        _runtest(test_edn_too_small, );
        _runtest(test_edn_nullptr, );
        _runtest(test_edn_large_enough_reuse, ryml2edn);
        _runtest(test_edn_too_small_reuse, ryml2edn);
        _runtest(test_edn_nullptr_reuse, ryml2edn);
        _runtest(test_evt_large_enough, );
        _runtest(test_evt_too_small, );
        _runtest(test_evt_nullptr, );
        _runtest(test_evt_large_enough_reuse, ryml2evt);
        _runtest(test_evt_too_small_reuse, ryml2evt);
        _runtest(test_evt_nullptr_reuse, ryml2evt);
        return tr;
    }

    // happy path: large-enough destination string
    TestResult test_edn_large_enough_reuse(Ryml2Edn *ryml2edn) const
    {
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        std::string output;
        output.resize(2 * edn.len);
        size_type reqsize = ys2edn_parse(ryml2edn, "ysfilename",
                                         input.str, (size_type)input.len,
                                         &output[0], (size_type)output.size());
        CHECK(reqsize == edn.len+1);
        CHECK(reqsize != 0);
        output.resize(reqsize - 1u);
        CHECK(testeq(c4::to_csubstr(output)));
        return tr;
    }
    TestResult test_evt_large_enough_reuse(Ryml2Evt *ryml2evt) const
    {
        if(evt.empty()) return {};
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        std::vector<evt::DataType> output;
        output.resize(2 * expected_size(evt));
        size_type reqsize = ys2evt_parse(ryml2evt, "ysfilename",
                                         input.str, (size_type)input.len,
                                         &output[0], (size_type)output.size());
        CHECK_MSG((size_t)reqsize == expected_size(evt), "%d vs %zu", reqsize, expected_size(evt));
        CHECK(reqsize != 0);
        output.resize(reqsize);
        CHECK(testeq(output, input));
        return tr;
    }
    TestResult test_edn_large_enough() const
    {
        Ys2EdnScoped lib;
        return test_edn_large_enough_reuse(lib.ryml2edn);
    }
    TestResult test_evt_large_enough() const
    {
        Ys2EvtScoped lib;
        return test_evt_large_enough_reuse(lib.ryml2evt);
    }

    // less-happy path: destination string not large enough
    TestResult test_edn_too_small_reuse(Ryml2Edn *ryml2edn) const
    {
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        std::string output = "?";
        size_type reqsize = ys2edn_parse(ryml2edn, "ysfilename",
                                         input.str, (size_type)input.len,
                                         output.data(), (size_type)output.size());
        CHECK(reqsize == edn.len+1);
        CHECK(reqsize != 0);
        CHECK(output != "?");
        output.resize(reqsize);
        size_type getsize = ys2edn_retry_get(ryml2edn, &output[0], (size_type)output.size());
        CHECK(getsize == reqsize);
        output.resize(reqsize - 1u);
        CHECK(testeq(c4::to_csubstr(output)));
        return tr;
    }
    TestResult test_evt_too_small_reuse(Ryml2Evt *ryml2evt) const
    {
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        std::vector<evt::DataType> output;
        output.resize(expected_size(evt));
        size_type reqsize = ys2evt_parse(ryml2evt, "ysfilename",
                                         input.str, (size_type)input.len,
                                         output.data(), (size_type)output.size());
        CHECK(reqsize == expected_size(evt));
        CHECK(reqsize != 0);
        output.resize(reqsize);
        input_.assign(ys.begin(), ys.end()); // FIXME
        input = c4::to_substr(input_);
        size_type reqsize2 = ys2evt_parse(ryml2evt, "ysfilename",
                                          input.str, (size_type)input.len,
                                          output.data(), (size_type)output.size());
        CHECK(reqsize2 == reqsize);
        output.resize(reqsize2);
        CHECK(testeq(output, input));
        return tr;
    }
    TestResult test_edn_too_small() const
    {
        Ys2EdnScoped lib;
        return test_edn_too_small_reuse(lib.ryml2edn);
    }
    TestResult test_evt_too_small() const
    {
        Ys2EvtScoped lib;
        return test_evt_too_small_reuse(lib.ryml2evt);
    }

    // safe calling with nullptr
    TestResult test_edn_nullptr_reuse(Ryml2Edn *ryml2edn) const
    {
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        size_type reqsize = ys2edn_parse(ryml2edn, "ysfilename",
                                         input.str, (size_type)input.len,
                                         nullptr, 0);
        CHECK(reqsize == edn.len+1);
        CHECK(reqsize != 0);
        return tr;
    }
    TestResult test_evt_nullptr_reuse(Ryml2Evt *ryml2evt) const
    {
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        size_type reqsize = ys2evt_parse(ryml2evt, "ysfilename",
                                         input.str, (size_type)input.len,
                                         nullptr, 0);
        CHECK(reqsize == expected_size(evt));
        CHECK(reqsize != 0);
        std::vector<evt::DataType> output;
        output.resize(reqsize);
        input_.assign(ys.begin(), ys.end()); // FIXME
        input = c4::to_substr(input_);
        size_type reqsize2 = ys2evt_parse(ryml2evt, "ysfilename",
                                          input.str, (size_type)input.len,
                                          output.data(), (size_type)output.size());
        CHECK(reqsize2 == reqsize);
        CHECK(reqsize2 == output.size());
        CHECK(testeq(output, input));
        return tr;
    }
    TestResult test_edn_nullptr() const
    {
        Ys2EdnScoped lib;
        return test_edn_nullptr_reuse(lib.ryml2edn);
    }
    TestResult test_evt_nullptr() const
    {
        Ys2EvtScoped lib;
        return test_evt_nullptr_reuse(lib.ryml2evt);
    }

public:

    bool testeq(csubstr actual) const
    {
        const bool status = (actual == edn);
        if(!status)
            printf("------\n"
                   "FAIL:\n"
                   "input:[%zu]~~~%.*s~~~\n"
                   "expected:[%zu]~~~%.*s~~~\n"
                   "actual:[%zu]~~~%.*s~~~\n",
                   ys.len, (int)ys.len, ys.str,
                   edn.len, (int)edn.len, edn.str,
                   actual.len, (int)actual.len, actual.str);
        return status;
    }

    bool testeq(std::vector<evt::DataType> const& actual, csubstr parsed_source) const
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
        for(size_t i = 0, ie = 0; ie < num_events_expected; ++ie)
        {
            if(i >= actual.size())
            {
                printf("fail: bad actual size. i=%zu vs %zu=actual.size()=\n", i, actual.size());
                status = false;
                break;
            }
            #define _testcmp(fmt, cmp, ...) \
                if(showcmp) { printf("status=%d cmp=%d evt=%zu i=%zu: " fmt "\n", status, (cmp), ie, i, ## __VA_ARGS__); } \
                status &= (cmp)
            char actualbuf[100];
            char expectedbuf[100];
            size_t reqsize_actual = c4::bm2str<evt::EventFlags>(actual[i] & evt::MASK, actualbuf, sizeof(actualbuf));
            size_t reqsize_expected = c4::bm2str<evt::EventFlags>(evt[ie].flags & evt::MASK, expectedbuf, sizeof(expectedbuf));
            C4_CHECK(reqsize_actual < sizeof(actualbuf));
            C4_CHECK(reqsize_expected < sizeof(expectedbuf));
            _testcmp("exp=%d(%s) vs act=%d(%s)", evt[ie].flags == actual[i], evt[ie].flags, expectedbuf, actual[i], actualbuf);
            status &= (evt[ie].flags == actual[i]);
            if((evt[ie].flags & evt::HAS_STR) && (actual[i] & evt::HAS_STR))
            {
                _testcmp("   exp=%d vs act=%d", evt[ie].str_start == actual[i + 1], evt[ie].str_start, actual[i + 1]);
                _testcmp("   exp=%d vs act=%d", evt[ie].str_len == actual[i + 2], evt[ie].str_len, actual[i + 2]);
                bool safeactual = (i + 2 < actual.size()) && (actual[i + 1] < (int)parsed_source.len && actual[i + 1] + actual[i + 2] <= (int)parsed_source.len);
                bool safeexpected = (evt[ie].str_start < (int)parsed_source.len && evt[ie].str_start + evt[ie].str_len <= (int)parsed_source.len);
                _testcmp("   safeactual=%d", safeactual, safeactual);
                _testcmp("   safeactual=%d safeexpected=%d", safeactual == safeexpected, safeactual, safeexpected);
                if(safeactual && safeexpected)
                {
                    csubstr evtstr = parsed_source.sub((size_t)evt[ie].str_start, (size_t)evt[ie].str_len);
                    csubstr actualstr = parsed_source.sub((size_t)actual[i + 1], (size_t)actual[i + 2]);
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
            i += (actual[i] & evt::HAS_STR) ? 3 : 1;
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

namespace {
// make the declarations shorter
#define tc(ys, edn, ...) {ys, edn, std::vector<EvtWithScalar>(__VA_ARGS__)}
#define e(...) EvtWithScalar{__VA_ARGS__}
using namespace evt;
inline constexpr bool needs_filter = true;
const TestCase test_cases[] = {
    // case -------------------------------------------------
    tc("a: 1",
       R"((
{:+ "+MAP"}
{:+ "=VAL", := "a"}
{:+ "=VAL", := "1"}
{:+ "-MAP"}
{:+ "-DOC"}
)
)",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 0, 1, "a"),
           e(VAL_|SCLR|PLAI, 3, 1, "1"),
           e(EMAP),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc("say: 2 + 2",
       R"((
{:+ "+MAP"}
{:+ "=VAL", := "say"}
{:+ "=VAL", := "2 + 2"}
{:+ "-MAP"}
{:+ "-DOC"}
)
)",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 0, 3, "say"),
           e(VAL_|SCLR|PLAI, 5, 5, "2 + 2"),
           e(EMAP),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc("𝄞: ✅",
       R"((
{:+ "+MAP"}
{:+ "=VAL", := "𝄞"}
{:+ "=VAL", := "✅"}
{:+ "-MAP"}
{:+ "-DOC"}
)
)",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 0, 4, "𝄞"),
           e(VAL_|SCLR|PLAI, 6, 3, "✅"),
           e(EMAP),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc("[a, b, c]",
       R"((
{:+ "+SEQ", :flow true}
{:+ "=VAL", := "a"}
{:+ "=VAL", := "b"}
{:+ "=VAL", := "c"}
{:+ "-SEQ"}
{:+ "-DOC"}
)
)",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BSEQ|FLOW),
           e(VAL_|SCLR|PLAI, 1, 1, "a"),
           e(VAL_|SCLR|PLAI, 4, 1, "b"),
           e(VAL_|SCLR|PLAI, 7, 1, "c"),
           e(ESEQ),
           e(EDOC),
           e(ESTR),
       }),
    // case ------------------------------
    tc("[a: b]",
       R"((
{:+ "+SEQ", :flow true}
{:+ "+MAP", :flow true}
{:+ "=VAL", := "a"}
{:+ "=VAL", := "b"}
{:+ "-MAP"}
{:+ "-SEQ"}
{:+ "-DOC"}
)
)",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BSEQ|FLOW),
           e(VAL_|BMAP|FLOW),
           e(KEY_|SCLR|PLAI, 1, 1, "a"),
           e(VAL_|SCLR|PLAI, 4, 1, "b"),
           e(EMAP),
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
       R"((
{:+ "+MAP", :! "yamlscript/v0"}
{:+ "=VAL", := "foo"}
{:+ "+SEQ", :! ""}
{:+ "+MAP", :flow true}
{:+ "=VAL", := "x"}
{:+ "=VAL", := "y"}
{:+ "-MAP"}
{:+ "+SEQ", :flow true}
{:+ "=VAL", := "x"}
{:+ "=VAL", := "y"}
{:+ "-SEQ"}
{:+ "=VAL", := "foo"}
{:+ "=VAL", :' "foo"}
{:+ "=VAL", :$ "foo"}
{:+ "=VAL", :| "foo\n"}
{:+ "=VAL", :> "foo\n"}
{:+ "+SEQ", :flow true}
{:+ "=VAL", := "1"}
{:+ "=VAL", := "2"}
{:+ "=VAL", := "true"}
{:+ "=VAL", := "false"}
{:+ "=VAL", := "null"}
{:+ "-SEQ"}
{:+ "=VAL", :& "anchor-1", :! "tag-1", := "foobar"}
{:+ "-SEQ"}
{:+ "-MAP"}
{:+ "-DOC"}
{:+ "+DOC"}
{:+ "+MAP"}
{:+ "=VAL", := "another"}
{:+ "=VAL", := "doc"}
{:+ "-MAP"}
{:+ "-DOC"}
)
)",
       {
           e(BSTR),
           e(BDOC|EXPL),
           e(VAL_|TAG_, 5, 13, "yamlscript/v0"),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 19, 3, "foo"),
           e(VAL_|TAG_, 25, 0, ""),
           e(VAL_|BSEQ|BLCK),
           e(VAL_|BMAP|FLOW),
           e(KEY_|SCLR|PLAI, 29, 1, "x"),
           e(VAL_|SCLR|PLAI, 32, 1, "y"),
           e(EMAP),
           e(VAL_|BSEQ|FLOW),
           e(VAL_|SCLR|PLAI, 38, 1, "x"),
           e(VAL_|SCLR|PLAI, 41, 1, "y"),
           e(ESEQ),
           e(VAL_|SCLR|PLAI, 46, 3, "foo"),
           e(VAL_|SCLR|SQUO, 53, 3, "foo"),
           e(VAL_|SCLR|DQUO, 61, 3, "foo"),
           e(VAL_|SCLR|LITL, 70, 4, "foo\n", needs_filter),
           e(VAL_|SCLR|FOLD, 80, 4, "foo\n", needs_filter),
           e(VAL_|BSEQ|FLOW),
           e(VAL_|SCLR|PLAI, 89, 1, "1"),
           e(VAL_|SCLR|PLAI, 92, 1, "2"),
           e(VAL_|SCLR|PLAI, 95, 4, "true"),
           e(VAL_|SCLR|PLAI, 101, 5, "false"),
           e(VAL_|SCLR|PLAI, 108, 4, "null"),
           e(ESEQ),
           e(VAL_|TAG_, 127, 5, "tag-1"),
           e(VAL_|ANCH, 117, 8, "anchor-1"),
           e(VAL_|SCLR|PLAI, 133, 6, "foobar"),
           e(ESEQ),
           e(EMAP),
           e(EDOC),
           e(BDOC|EXPL),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 144, 7, "another"),
           e(VAL_|SCLR|PLAI, 153, 3, "doc"),
           e(EMAP),
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
       R"((
{:+ "+MAP"}
{:+ "=VAL", := "plain"}
{:+ "=VAL", := "well a b c"}
{:+ "=VAL", := "squo"}
{:+ "=VAL", :' "single'quote"}
{:+ "=VAL", := "dquo"}
{:+ "=VAL", :$ "x\t\ny"}
{:+ "=VAL", := "lit"}
{:+ "=VAL", :| "X\nY\nZ\n"}
{:+ "=VAL", := "fold"}
{:+ "=VAL", :> "U V W\n"}
{:+ "-MAP"}
{:+ "-DOC"}
)
)",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 0, 5, "plain"),
           e(VAL_|SCLR|PLAI, 7, 10, "well a b c"),
           e(KEY_|SCLR|PLAI, 24, 4, "squo"),
           e(VAL_|SCLR|SQUO, 31, 12, "single'quote", needs_filter),
           e(KEY_|SCLR|PLAI, 46, 4, "dquo"),
           e(VAL_|SCLR|DQUO, 53, 4, "x\t\ny", needs_filter),
           e(KEY_|SCLR|PLAI, 61, 3, "lit"),
           e(VAL_|SCLR|LITL, 68, 6, "X\nY\nZ\n", needs_filter),
           e(KEY_|SCLR|PLAI, 89, 4, "fold"),
           e(VAL_|SCLR|FOLD, 97, 6, "U V W\n", needs_filter),
           e(EMAP),
           e(EDOC),
           e(ESTR),
       }),
    // case -------------------------------------------------
    tc("- !!seq []",
       R"((
{:+ "+SEQ"}
{:+ "+SEQ", :! "tag:yaml.org,2002:int", := "42"}
{:+ "-SEQ"}
{:+ "-SEQ"}
{:+ "-DOC"}
)
)",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 0, 4, "𝄞"),
           e(VAL_|SCLR|PLAI, 6, 3, "✅"),
           e(EMAP),
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
       R"_((
{:+ "+MAP"}
{:+ "=VAL", := "defn run(prompt session=nil)"}
{:+ "+MAP"}
{:+ "=VAL", := "when session"}
{:+ "+MAP"}
{:+ "=VAL", := "write session _ :append true"}
{:+ "=VAL", :| "Q: $(orig-prompt:trim)\nA ($api-model):\n$(answer:trim)\n"}
{:+ "-MAP"}
{:+ "-MAP"}
{:+ "-MAP"}
{:+ "-DOC"}
)
)_",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 0, 28, "defn run(prompt session=nil)"),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 32, 12, "when session"),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 50, 28, "write session _ :append true"),
           e(VAL_|SCLR|LITL, 83, 54, "Q: $(orig-prompt:trim)\nA ($api-model):\n$(answer:trim)\n", needs_filter),
           e(EMAP),
           e(EMAP),
           e(EMAP),
           e(EDOC),
           e(ESTR),
       }),
#endif
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
       R"_((
{:+ "+MAP"}
{:+ "=VAL", := "defn run(prompt session=nil)"}
{:+ "+MAP"}
{:+ "=VAL", := "session-text ="}
{:+ "+MAP"}
{:+ "=VAL", := "when session && session:fs-e"}
{:+ "=VAL", :: ""}
{:+ "-MAP"}
{:+ "=VAL", := "answer ="}
{:+ "+MAP"}
{:+ "=VAL", := "cond"}
{:+ "+MAP"}
{:+ "=VAL", := "api-model =~ /^dall-e/"}
{:+ "=VAL", := "openai-image(prompt).data.0.url"}
{:+ "=VAL", := "api-model.in?(anthropic-models)"}
{:+ "=VAL", := "anthropic(prompt):anthropic-message:format"}
{:+ "=VAL", := "api-model.in?(groq-models)"}
{:+ "=VAL", := "groq(prompt).choices.0.message.content:format"}
{:+ "=VAL", := "api-model.in?(openai-models)"}
{:+ "=VAL", := "openai-chat(prompt).choices.0.message.content:format"}
{:+ "=VAL", := "else"}
{:+ "=VAL", := "die()"}
{:+ "-MAP"}
{:+ "-MAP"}
{:+ "=VAL", := "say"}
{:+ "=VAL", := "answer"}
{:+ "=VAL", := "when session"}
{:+ "+MAP"}
{:+ "=VAL", := "write session _ :append true"}
{:+ "=VAL", :| "Q: $(orig-prompt:trim)\nA ($api-model):\n$(answer:trim)\n\n"}
{:+ "-MAP"}
{:+ "-MAP"}
{:+ "-MAP"}
{:+ "-DOC"}
)
)_",
       {
           e(BSTR),
           e(BDOC),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 21, 28, "defn run(prompt session=nil)"),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 53, 14, "session-text ="),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 73, 28, "when session && session:fs-e"),
           e(VAL_|SCLR|PLAI, 0, 0, ""), // note empty scalar pointing at the front
           e(EMAP),
           e(KEY_|SCLR|PLAI, 106, 8, "answer ="),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 120, 4, "cond"),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 132, 22, "api-model =~ /^dall-e/"),
           e(VAL_|SCLR|PLAI, 164, 31, "openai-image(prompt).data.0.url"),
           e(KEY_|SCLR|PLAI, 202, 31, "api-model.in?(anthropic-models)"),
           e(VAL_|SCLR|PLAI, 243, 42, "anthropic(prompt):anthropic-message:format"),
           e(KEY_|SCLR|PLAI, 292, 26, "api-model.in?(groq-models)"),
           e(VAL_|SCLR|PLAI, 328, 45, "groq(prompt).choices.0.message.content:format"),
           e(KEY_|SCLR|PLAI, 380, 28, "api-model.in?(openai-models)"),
           e(VAL_|SCLR|PLAI, 418, 52, "openai-chat(prompt).choices.0.message.content:format"),
           e(KEY_|SCLR|PLAI, 477, 4, "else"),
           e(VAL_|SCLR|PLAI, 483, 5, "die()"),
           e(EMAP),
           e(EMAP),
           e(KEY_|SCLR|PLAI, 492, 3, "say"),
           e(VAL_|SCLR|PLAI, 497, 6, "answer"),
           e(KEY_|SCLR|PLAI, 507, 12, "when session"),
           e(VAL_|BMAP|BLCK),
           e(KEY_|SCLR|PLAI, 525, 28, "write session _ :append true"),
           e(VAL_|SCLR|LITL, 558, 55, "Q: $(orig-prompt:trim)\nA ($api-model):\n$(answer:trim)\n\n", needs_filter),
           e(EMAP),
           e(EMAP),
           e(EMAP),
           e(EDOC),
           e(ESTR),
       }),
};
} // namespace

int main()
{
    Ys2EdnScoped ys2edn;
    Ys2EvtScoped ys2evt;
    TestResult total = {};
    size_t failed_cases = {};
    size_t num_cases = C4_COUNTOF(test_cases);
    for(size_t i = 0; i < C4_COUNTOF(test_cases); ++i)
    {
        printf("-----------------------------------------\n"
               "case %zu/%zu ...\n"
               "[%zu]~~~%.*s~~~\n", i, num_cases, test_cases[i].ys.len, (int)test_cases[i].ys.len, test_cases[i].ys.str);
        const TestResult tr = test_cases[i].test(ys2edn.ryml2edn, ys2evt.ryml2evt);
        total.add(tr);
        failed_cases += (!tr);
        printf("case %zu/%zu: %s\n", i, C4_COUNTOF(test_cases), tr ? "ok!" : "failed");
    }
    printf("assertions: %u/%u pass %u/%u fail\n", total.num_assertions - total.num_failed_assertions, total.num_assertions, total.num_failed_assertions, total.num_assertions);
    printf("tests: %u/%u pass %u/%u fail\n", total.num_tests - total.num_failed_tests, total.num_tests, total.num_failed_tests, total.num_tests);
    printf("cases: %zu/%zu pass %zu/%zu fail\n", num_cases-failed_cases, num_cases, failed_cases, num_cases);
    if(total)
        printf("TESTS SUCCEED!\n");
    return total ? 0 : -1;
}
