#include <rapidyaml_edn.hpp>

struct Ys2EvtsScoped
{
    std::string input;
    char *evts;
    Ys2EvtsScoped(const char *src) : input(src), evts() { evts = ys2edn_create(input.data()); }
    ~Ys2EvtsScoped() { if(evts) ys2edn_destroy(evts); }
};

struct TestCase
{
    const char *input_src;
    const char *expected_evts;
    bool test() const
    {
        Ys2EvtsScoped result(input_src);
        const bool status = strcmp(result.evts, expected_evts) == 0;
        if(!status)
            printf("------\n"
                   "FAIL:\n"
                   "input:~~~%s~~~\n"
                   "expected:~~~%s~~~\n"
                   "actual:~~~%s~~~\n",
                   input_src, expected_evts, result.evts);
        return status;
    }
};

const TestCase test_cases[] = {
    {"a: 1", "{:a 1}"},
    {"𝄞: ✅", "{:𝄞 ✅}"},
    {
        R"(foo: !
- {x: y}
- [x, y]
- foo
- 'foo'
- "foo"
- |
  foo
- >
  foo
- &anchor-1 !tag-1 foobar
)",
        R"(({:+ "+MAP", :! "yamlscript/v0"}
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
 {:+ "=VAL", :& "anchor-1", :! "tag-1", := "foobar"}
 {:+ "-SEQ"}
 {:+ "-MAP"}
 {:+ "-DOC"}))"}
};

int main()
{
    int result = 0;
    for(size_t i = 0; i < C4_COUNTOF(test_cases); ++i)
    {
        printf("case %zu/%zu ...\n", i, C4_COUNTOF(test_cases));
        bool ok = true;
        if(!test_cases[i].test()) {
            result = -1;
            ok = false;
        }
        printf("case %zu/%zu: %s\n", i, C4_COUNTOF(test_cases), ok ? "ok!" : "failed");
    }
    return result;
}
