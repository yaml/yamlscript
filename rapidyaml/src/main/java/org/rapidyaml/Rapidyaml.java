package org.rapidyaml;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

/**
 * Interface with the shared librapidyaml library
 *
 */
public class Rapidyaml {
    public static String RAPIDYAML_VERSION = "0.7.0";

    private native long ys2edn_init();
    private native void ys2edn_destroy(long ryml2edn);
    private native int ys2edn_parse(long ryml2edn, String filename,
                                    byte[] ys, int ys_length,
                                    byte[] edn, int edn_length);
    private native int ys2edn_retry_get(
        long ryml2edn, byte[] edn, int edn_size
    );

    private final long ryml2edn;

    public Rapidyaml() {
        String library_name = "rapidyaml"; // ." + RAPIDYAML_VERSION;
        System.loadLibrary(library_name);
        this.ryml2edn = this.ys2edn_init();
    }

    // Likely bad idea to implement finalize:
    //
    // https://stackoverflow.com/questions/158174/why-would-you-ever-implement-finalize
    //
    protected void finalize() throws Throwable {
        try {
            this.ys2edn_destroy(this.ryml2edn);
        }
        finally {
            super.finalize();
        }
    }

    public void testit()
    {
        String ys = ": : : :";
        boolean gotit = false;
        try {
            this.parseYS(ys);
        }
        catch (RuntimeException e) {
            gotit = true;
        }
        catch (Exception e) {
            gotit = false;
        }
        if (!gotit)
            throw new RuntimeException("wtf");
    }

    public String parseYS(String srcstr) throws RuntimeException {
        String filename = "yamlscript"; // fixme
        byte[] src = srcstr.getBytes(StandardCharsets.UTF_8);
        int edn_size = 10 * src.length;
        byte[] edn = new byte[edn_size];
        int required_size = ys2edn_parse(this.ryml2edn, filename, src, src.length, edn, edn_size);
        if(required_size > edn_size) {
            edn_size = required_size;
            edn = new byte[edn_size];
            required_size = ys2edn_retry_get(this.ryml2edn, edn, edn_size);
            if(required_size != edn_size) {
                throw new RuntimeException("inconsistent size");
            }
        }
        String ret = new String(edn, 0, required_size-1, StandardCharsets.UTF_8);
        return ret;
    }
}
