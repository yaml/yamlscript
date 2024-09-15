package org.rapidyaml;

import org.rapidyaml.YamlParseErrorException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

/**
 * Interface with the shared librapidyaml library
 *
 */
public class Rapidyaml {
    public static String RAPIDYAML_VERSION = "0.7.2";

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

    /*
    using FlagsType = int32_t;
    enum : FlagsType {
        // ---------------------
        // scalar flags
        SCLR = 1 <<  0,   // =VAL
        PLAI = 1 <<  1,   // : (plain scalar)
        SQUO = 1 <<  2,   // ' (single-quoted scalar)
        DQUO = 1 <<  3,   // " (double-quoted scalar)
        LITL = 1 <<  4,   // | (block literal scalar)
        FOLD = 1 <<  5,   // > (block folded scalar)
        // ---------------------
        // container flags
        BSEQ = 1 <<  6,   // +SEQ (Begin SEQ)
        ESEQ = 1 <<  7,   // -SEQ (End   SEQ)
        BMAP = 1 <<  8,   // +MAP
        EMAP = 1 <<  9,   // -MAP
        FLOW = 1 << 10,   // flow container: [] for seqs or {} for maps
        BLCK = 1 << 11,   // block container
        // ---------------------
        // document flags
        BDOC = 1 << 12,   // +DOC
        EDOC = 1 << 13,   // -DOC
        BSTR = 1 << 14,   // +STR
        ESTR = 1 << 15,   // -STR
        EXPL = 1 << 16,   // --- (with BDOC) or ... (with EDOC) (may be fused with FLOW if needed)
        // ---------------------
        // other flags
        ALIA = 1 << 19    // ref
        ANCH = 1 << 18,   // anchor
        TAG_ = 1 << 20,   // tag
    }
    struct ParseEvent
    {
        FlagsType flags;
        int32_t strStart;
        int32_t strEnd;
        // anchors: either this:
        int32_t anchorStart;
        int32_t anchorEnd;
        // ... or this, which saves space when anchors are rare,
        // and has the advantage of making the event have 4 words:
        //int32_t anchorId; // counter, pointing at a different
                            // array of { int32_t anchorStart, anchorEnd; }
    };
    */

    public String parseYS(String srcstr) throws RuntimeException, org.rapidyaml.YamlParseErrorException {
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
