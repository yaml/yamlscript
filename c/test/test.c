#include <yamlscript_c.h>
#include <stddef.h>

int main()
{
    const char ys[] = ""
        "!yamlscript/v0\n"
        "\n"
        "name =: \"World\"\n"
        "\n"
        "=>::\n"
        "  foo: [1, 2, ! inc(41)]\n"
        "  bar:: load(\"other.yaml\")\n"
        "  baz:: \"Hello, $name!\"\n"
        "";
    int sz = 0;
    char json[1024];
    yamlscript_errcode ec = yamlscript_load_ys_to_json(ys, json, (int)sizeof(json), &sz);
    return ec;
}
