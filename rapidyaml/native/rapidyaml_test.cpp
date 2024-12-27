#include <rapidyaml_edn.hpp>
#include <rapidyaml_evt.hpp>

using c4::csubstr;
using c4::substr;
using c4::substr;


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


struct TestResult
{
    uint32_t num_assertions;
    uint32_t num_tests;
    uint32_t num_failed_assertions;
    uint32_t num_failed_tests;
    operator bool() const { return num_failed_tests == 0; }
    void add(TestResult const& that)
    {
        num_tests += 1 + that.num_tests;
        num_assertions += that.num_assertions;
        num_failed_tests += (that.num_failed_assertions > 0) + that.num_failed_tests;
        num_failed_assertions += that.num_failed_assertions;
    }
};


struct TestCase
{
    csubstr ys;
    csubstr edn;
    c4::cspan<ParseEvent> evt;

public:
    bool testeq(csubstr actual) const
    {
        const bool status = (actual == edn);
        if(!status)
            printf("------\n"
                   "FAIL:\n"
                   "input:~~~%.*s~~~\n"
                   "expected:~~~%.*s~~~\n"
                   "actual:~~~%.*s~~~\n",
                   (int)ys.len, ys.str,
                   (int)edn.len, edn.str,
                   (int)actual.len, actual.str);
        return status;
    }
    bool testeq(std::vector<ParseEvent> const& actual_) const
    {
        c4::cspan<ParseEvent> actual(actual_.data(), actual_.size());
        if(actual.size() != evt.size())
        {
            printf("------\n"
                   "FAIL:\n"
                   "input:~~~%.*s~~~\n"
                   "expected size:~~~%zu~~~\n"
                   "actual size:~~~%zu~~~\n",
                   (int)ys.len, ys.str,
                   evt.size(),
                   actual.size());
            return false;
        }
        int status = true;
        for(size_t i = 0; i < actual.size(); ++i)
        {
            printf("wtf! i=%zu status=%d: %d vs %d=%d\n", i, status, evt[i].flags, actual[i].flags, evt[i].flags == actual[i].flags);
            status &= (&evt[i]          != &actual[i]);
            status &= (evt[i].flags     == actual[i].flags);
            if((evt[i].flags & (SCLR|ALIA|ANCH|TAG_)) && (evt[i].flags == actual[i].flags))
            {
                printf("wtf! i=%zu status=%d: %d vs %d=%d\n", i, status, evt[i].str_start, actual[i].str_start, evt[i].str_start == actual[i].str_start);
                printf("wtf! i=%zu status=%d: %d vs %d=%d\n", i, status, evt[i].str_len, actual[i].str_len, evt[i].str_len == actual[i].str_len);
                status &= (evt[i].str_start == actual[i].str_start);
                status &= (evt[i].str_len   == actual[i].str_len);
            }
        }
        if(!status)
            printf("------\n"
                   "FAIL:\n"
                   "input:~~~%.*s~~~\n",
                   (int)ys.len, ys.str);
        return status;
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
                printf("%s:%d: fail! %s\n", __FILE__, __LINE__, #cond);   \
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
        std::vector<ParseEvent> output;
        output.resize(2 * evt.size());
        size_type reqsize = ys2evt_parse(ryml2evt, "ysfilename",
                                         input.str, (size_type)input.len,
                                         &output[0], (size_type)output.size());
        CHECK(reqsize == evt.size());
        CHECK(reqsize != 0);
        output.resize(reqsize);
        CHECK(testeq(output));
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
if(evt.empty()) return {};
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        std::vector<ParseEvent> output;
        output.resize(evt.size() / 2);
        size_type reqsize = ys2evt_parse(ryml2evt, "ysfilename",
                                         input.str, (size_type)input.len,
                                         output.data(), (size_type)output.size());
        CHECK(reqsize == evt.size());
        CHECK(reqsize != 0);
        output.resize(reqsize);
        size_type reqsize2 = ys2evt_parse(ryml2evt, "ysfilename",
                                          input.str, (size_type)input.len,
                                          output.data(), (size_type)output.size());
        CHECK(reqsize2 == reqsize);
        output.resize(reqsize2);
        CHECK(testeq(output));
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
if(evt.empty()) return {};
        TestResult tr = {};
        std::string input_(ys.begin(), ys.end());
        substr input = c4::to_substr(input_);
        size_type reqsize = ys2evt_parse(ryml2evt, "ysfilename",
                                         input.str, (size_type)input.len,
                                         nullptr, 0);
        CHECK(reqsize == evt.size());
        CHECK(reqsize != 0);
        std::vector<ParseEvent> output;
        output.resize(reqsize);
        size_type reqsize2 = ys2evt_parse(ryml2evt, "ysfilename",
                                          input.str, (size_type)input.len,
                                          output.data(), (size_type)output.size());
        CHECK(reqsize2 == reqsize);
        CHECK(reqsize2 == output.size());
        CHECK(testeq(output));
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
};


//-----------------------------------------------------------------------------

#define tc(ys, edn, ...) {ys, edn, c4::cspan<ParseEvent>(__VA_ARGS__)}
constexpr ParseEvent e(EventFlagsType flags) { return {flags, 0, 0}; }
constexpr ParseEvent e(EventFlagsType flags, int start, int end) { return {flags, start, end}; }
const TestCase test_cases[] = {
    // case ------------------------------
    tc("a: 1",
     R"((
{:+ "+MAP"}
{:+ "=VAL", := "a"}
{:+ "=VAL", := "1"}
{:+ "-MAP"}
{:+ "-DOC"}
)
)", {
             e(BSTR),
             e(BDOC),
             e(BMAP|BLCK|VAL_),
             e(KEY_|SCLR|PLAI, 0, 1),
             e(VAL_|SCLR|PLAI, 3, 1),
             e(EMAP),
             e(EDOC),
             e(ESTR),
       }),
    // case ------------------------------
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
             e(BMAP|BLCK|VAL_),
             e(KEY_|SCLR|PLAI, 0, 3),
             e(VAL_|SCLR|PLAI, 5, 5),
             e(EMAP),
             e(EDOC),
             e(ESTR),
       }),
    // case ------------------------------
    tc("𝄞: ✅",
     R"((
{:+ "+MAP"}
{:+ "=VAL", := "𝄞"}
{:+ "=VAL", := "✅"}
{:+ "-MAP"}
{:+ "-DOC"}
)
)", {}),
    // case ------------------------------
    tc("[a, b, c]",
     R"((
{:+ "+SEQ", :flow true}
{:+ "=VAL", := "a"}
{:+ "=VAL", := "b"}
{:+ "=VAL", := "c"}
{:+ "-SEQ"}
{:+ "-DOC"}
)
)", {}),
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
)", {}),
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
)", {}),
};


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
