package org.rapidyaml;

import java.net.URL;
import java.security.CodeSource;

import org.rapidyaml.NativeLibLoader;
import java.io.IOException;

import org.rapidyaml.YamlParseErrorException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;

/**
 * Interface with the librapidyaml shared library
 */
public class Rapidyaml
{
    public static String RAPIDYAML_NAME = "rapidyaml";
    public static String RAPIDYAML_VERSION = "0.8.0";
    public static String RAPIDYAML_LIBNAME =
        String.format(
            "%s.%s",
            RAPIDYAML_NAME,
            RAPIDYAML_VERSION);

    private native long ysparse_init();
    private native void ysparse_destroy(long ysparse);
    private native void ysparse_timing_set(boolean yes);
    private native int ysparse_parse(
        long ysparse, String filename,
        byte[] ys, int ys_length,
        int[] evt, int evt_length);
    private native int ysparse_parse_buf(
        long ysparse, String filename,
        ByteBuffer ys, int ys_length,
        IntBuffer evt, int evt_length);

    private final long ysparse;



            // XXX this 'main' is for testing
            public static void main(
                String[] args
            ) throws Exception, IOException {
                (new Rapidyaml()).timingEnabled(true);
                System.out.printf("It works!\n");
            }



    //------------------------
    // CTOR/DTOR
    //------------------------

    public Rapidyaml() throws Exception, IOException {
        System.loadLibrary(RAPIDYAML_LIBNAME);
        this.ysparse = this.ysparse_init();
        timingEnabled(false);
    }

    // Likely bad idea to implement finalize:
    //
    // https://stackoverflow.com/questions/158174/why-would-you-ever-implement-finalize
    //
    protected void finalize() throws Throwable {
        try {
            this.ysparse_destroy(this.ysparse);
        }
        finally {
            super.finalize();
        }
    }


    //------------------------
    // EVT
    //------------------------

    public int parseYsToEvt(
        byte[] src, int[] evts
    ) throws Exception {
        return parseYsToEvt("yamlscript", src, evts);
    }

    public int parseYsToEvtBuf(
        ByteBuffer src, IntBuffer evt
    ) throws Exception {
        return parseYsToEvtBuf("yamlscript", src, evt);
    }

    public int parseYsToEvt(
        String filename, byte[] src, int[] evts
    ) throws Exception {
        long t = timingStart("ysparse");
        int required_size = ysparse_parse(
            this.ysparse, filename,
            src, src.length, evts, evts.length);
        timingStop("ysparse", t, src.length);
        return required_size;
    }

    public int parseYsToEvtBuf(
        String filename, ByteBuffer src, IntBuffer evt
    ) throws Exception {
        if (! src.isDirect())
            throw new RuntimeException("src must be direct");
        if (! evt.isDirect())
            throw new RuntimeException("evt must be direct");
        // the byte order for src does not matter
        // but for evt it really does
        if (evt.order() != ByteOrder.nativeOrder())
            throw new RuntimeException("evt byte order must be native");
        long t = timingStart("ysparseBuf");
        evt.position(evt.capacity());
        int reqsize = ysparse_parse_buf(
            this.ysparse, filename,
            src, src.position(), evt, evt.capacity());
        if (reqsize <= evt.capacity()) {
            evt.position(reqsize);
        }
        timingStop("ysparseBuf", t, src.position());
        return reqsize;
    }

    public static IntBuffer mkIntBuffer(int numInts) {
        ByteBuffer bb = ByteBuffer.allocateDirect(/*numBytes*/4 * numInts);
        // !!! need to explicitly set the byte order to the native order
        return bb.order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    //------------------------
    // TIME
    //------------------------

    private boolean showTiming = true;

    public void timingEnabled(boolean yes) {
        showTiming = yes;
        ysparse_timing_set(yes);
    }

    private long timingStart(String name) {
        if(showTiming) {
            System.out.printf("     java:%s...\n", name);
            return System.nanoTime();
        }
        return 0;
    }

    private void timingStop(String name, long t) {
        if(showTiming) {
            t = System.nanoTime() - t;
            System.out.printf(
                "     java:%s: %.6fms\n", name, (float) t/1.e6f);
        }
    }

    private void timingStop(String name, long t, int numBytes) {
        if(showTiming) {
            t = System.nanoTime() - t;
            float dt = (float)t;
            float fb = (float)numBytes;
            System.out.printf(
                "     java:%s: %.6fms  %.3fMB/s  %dB\n",
                name, dt/1.e6f, 1.e3f*fb/dt, numBytes);
        }
    }
}
