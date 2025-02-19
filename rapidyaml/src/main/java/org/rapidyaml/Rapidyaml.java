package org.rapidyaml;

import org.rapidyaml.YamlParseErrorException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

/**
 * Interface with the shared librapidyaml library
 */
public class Rapidyaml {
    public static String RAPIDYAML_VERSION = "0.8.0";

    private native long ys2edn_init();
    private native void ys2edn_destroy(long ryml2edn);
    private native int ys2edn_parse(long ryml2edn, String filename,
                                    byte[] ys, int ys_length,
                                    byte[] edn, int edn_length);
    private native int ys2edn_retry_get(
        long ryml2edn, byte[] edn, int edn_size
    );
    private final long ryml2edn;

    private native long ys2evt_init();
    private native void ys2evt_destroy(long ryml2evt);
    private native int ys2evt_parse(long ryml2evt, String filename,
                                    byte[] ys, int ys_length,
                                    int[] evt, int evt_length);
    private final long ryml2evt;

    public Rapidyaml() {
        String library_name = "rapidyaml"; // ." + RAPIDYAML_VERSION;
        System.loadLibrary(library_name);
        this.ryml2edn = this.ys2edn_init();
        this.ryml2evt = this.ys2evt_init();
    }

    // Likely bad idea to implement finalize:
    //
    // https://stackoverflow.com/questions/158174/why-would-you-ever-implement-finalize
    //
    protected void finalize() throws Throwable {
        try {
            this.ys2edn_destroy(this.ryml2edn);
            this.ys2evt_destroy(this.ryml2evt);
        }
        finally {
            super.finalize();
        }
    }

    public String parseYsToEdn(String srcstr)
        throws RuntimeException, org.rapidyaml.YamlParseErrorException
    {
        String filename = "yamlscript"; // fixme
long t = System.nanoTime();
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
t = System.nanoTime() - t;
System.out.printf("     edn@java=%.6fms\n", (double)t / 1.e6);
        String ret = new String(edn, 0, required_size-1, StandardCharsets.UTF_8);
        return ret;
    }

    public int parseYsToEvt(byte[] src, int[] evts)
        throws RuntimeException, org.rapidyaml.YamlParseErrorException
    {
        String filename = "yamlscript"; // fixme
long t = System.nanoTime();
        int required_size = ys2evt_parse(this.ryml2evt, filename, src, src.length, evts, evts.length);
t = System.nanoTime() - t;
System.out.printf("     evt@java=%.6fms\n", (double)t / 1.e6);
        return required_size;
    }
}
