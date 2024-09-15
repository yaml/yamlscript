#include <rapidyaml_edn.hpp>
#include <stdio.h>


size_t file_get_contents(const char *filename, char *buf, size_t sz, const char* access="rb")
{
    ::FILE *fp = ::fopen(filename, access);
    C4_CHECK_MSG(fp != nullptr, "could not open file %s", filename);
    ::fseek(fp, 0, SEEK_END);
    const size_t fs = static_cast<size_t>(::ftell(fp));
    ::rewind(fp);
    if(fs <= sz && buf != nullptr)
    {
        if(fs != ::fread(buf, 1, fs, fp))
        {
            ::fclose(fp);
            C4_ERROR("failed to read");
        }
    }
    C4_CHECK(::fclose(fp) == 0);
    return fs;
}

void file_get_contents(const char *filename, std::string *s)
{
    size_t sz = file_get_contents(filename, &(*s)[0], s->size());
    if(sz > s->size())
    {
        s->resize(sz);
        file_get_contents(filename, &(*s)[0], s->size());
    }
}

std::string file_get_contents(const char *filename, size_t minsize=2048)
{
    std::string s;
    s.resize(minsize);
    file_get_contents(filename, &s);
    return s;
}


struct Args
{
    const char *filename = nullptr;
    bool emit = true;
    static void print_usage(const char *progname)
    {
        const char *basename = c4::to_csubstr(progname).basename().str;
        printf("USAGE:\n"
               "\n"
               "    %s [-h|-s] <filename>\n"
               "\n"
               "Options:\n"
               "    -h,--help       print this help\n"
               "    -ne,--no-emit   only parse, do not emit EDN\n"
               "\n", basename);
    }
    bool load(int argc, const char *argv[])
    {
        if(argc == 1) {
            print_usage(argv[0]);
            printf("ERROR: missing filename\n");
            return false;
        }
        for(int i = 1; i < argc - 1; ++i)
        {
            c4::csubstr a = c4::to_csubstr(argv[i]);
            if(a == "-h" || a == "--help")
            {
                print_usage(argv[0]);
                return false;
            }
            else if(a == "-ne" || a == "--no-emit")
            {
                emit = false;
            }
            else
            {
                print_usage(argv[0]);
                printf("ERROR: unknown option: '%s'\n", argv[i]);
                return false;
            }
        }
        filename = argv[argc - 1];
        return true;
    }
};

int main(int argc, const char *argv[])
{
    Args args;
    if(!args.load(argc, argv))
        return 1;
    std::string file_contents = file_get_contents(args.filename);
    std::string edn;
    edn.resize(file_contents.size() * 4);
    Ryml2Edn *handle = ys2edn_init();
    size_t edn_size = ys2edn_parse(handle, args.filename, &file_contents[0], file_contents.size(), &edn[0], edn.size());
    if(edn_size > edn.size())
    {
        edn.resize(edn_size);
        ys2edn_retry_get(handle, &edn[0], edn.size());
    }
    if(args.emit)
        printf("%s\n", edn.c_str());
    ys2edn_destroy(handle);
    return 0;
}
