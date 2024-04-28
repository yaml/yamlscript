package org.rapidyaml;

/**
 * Interface with the shared librapidyaml library
 *
 */
public class Rapidyaml
{
    public static String RAPIDYAML_VERSION = "0.6.0";

    private final ILibRapidyaml librapidyaml;

    public Rapidyaml()
    {
        this.librapidyaml = LibRapidyaml.library();
    }

    public String parseYS(String src) throws RuntimeException
    {
        // TODO use a StringBuffer, and do not allocate in C++
        return librapidyaml.ys2evts_create(src);
    }

}
