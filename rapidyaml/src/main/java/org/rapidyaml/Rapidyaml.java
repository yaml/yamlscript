package org.rapidyaml;

import com.sun.jna.Pointer;
import com.sun.jna.Native;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

/**
 * Interface with the shared librapidyaml library
 *
 */
public class Rapidyaml
{
    public static String RAPIDYAML_VERSION = "0.7.0";

    private final ILibRapidyaml librapidyaml;
    private final Pointer ryml2edn;

    public Rapidyaml()
    {
        this.librapidyaml = LibRapidyaml.library();
        this.ryml2edn = this.librapidyaml.ys2edn_init();
    }

    protected void finalize() throws Throwable
    {
        try
        {
            this.librapidyaml.ys2edn_destroy(this.ryml2edn);
        }
        finally
        {
            super.finalize();
        }
    }

    public String parseYS0(String srcstr) throws RuntimeException
    {
        // using ys2edn_alloc(): allocates the output in C++
        String filename = "yamlscript"; // fixme
        // see https://stackoverflow.com/questions/34159610/jna-how-to-pass-a-string-as-void-from-java-to-c
        // 1. works:
        //return librapidyaml.ys2edn_alloc(this.ryml2edn, filename, srcstr, srcstr.length());
        // 2. crashes:
        //byte[] src = Native.toByteArray(srcstr, StandardCharsets.UTF_8);
        //return librapidyaml.ys2edn_alloc(this.ryml2edn, filename, src, src.length);
        // 3. works:
        byte[] src = srcstr.getBytes(StandardCharsets.UTF_8);
        return librapidyaml.ys2edn_alloc(this.ryml2edn, filename, src, src.length);
    }

    public String parseYS(String srcstr) throws RuntimeException
    {
        String filename = "yamlscript"; // fixme
        byte[] src = srcstr.getBytes(StandardCharsets.UTF_8);
        int edn_size = 10 * src.length;
        byte[] edn = new byte[edn_size];
        int required_size = librapidyaml.ys2edn(this.ryml2edn, filename, src, src.length, edn, edn_size);
        if(required_size > edn_size)
        {
            edn_size = required_size;
            edn = new byte[edn_size];
            required_size = librapidyaml.ys2edn_retry_get(this.ryml2edn, edn, edn_size);
            if(required_size != edn_size)
            {
                throw new RuntimeException("inconsistent size");
            }
        }
        String ret = new String(edn, 0, required_size-1, StandardCharsets.UTF_8);
        return ret;
    }

}
