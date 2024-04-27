package org.ryml.ryml;

/**
 * Interface with the shared libryml library
 *
 */
public class Ryml
{
    public static String RYML_VERSION = "0.5.0";

    private final ILibRyml libryml;

    public Ryml()
    {
        this.libryml = LibRyml.library();
    }

    public String parseYS(String src) throws RuntimeException
    {
        String jsonData = libryml.parse_yamlscript_to_events(src);
        return jsonData;
    }

}
