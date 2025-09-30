package org.rapidyaml;

import org.rapidyaml.NativeLibLoader;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;

/**
 * Interface with the librapidyaml shared library
 */
public class Rapidyaml {
    public static String RAPIDYAML_NAME = "ysparse.0.9.0";
    public static String RAPIDYAML_LIBNAME = "ysparse.0.9.0.so";

    private final long ysparse; ///< pointer to the c++ object
    private native long ysparse_init();
    private native void ysparse_destroy(long ysparse);
    private native void ysparse_timing_set(boolean yes);
    private native boolean ysparse_parse(long ysparse, String filename,
                                         byte[] ys, int ys_length,
                                         byte[] arena, int arena_length,
                                         int[] evt, int evt_length);
    private native boolean ysparse_parse_buf(long ysparse, String filename,
                                             ByteBuffer ys, int ys_length,
                                             ByteBuffer arena, int arena_length,
                                             IntBuffer evt, int evt_length);
    private native int ysparse_reqsize_evt(long ysparse);
    private native int ysparse_reqsize_arena(long ysparse);


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
        if (System.getenv("YS_TESTING") != null)
            System.loadLibrary(RAPIDYAML_NAME);
        else
            NativeLibLoader.loadLibraryFromResource(RAPIDYAML_LIBNAME);

        this.ysparse = this.ysparse_init();
        timingEnabled(false);
    }

    // Likely bad idea to implement finalize:
    //
    // https://stackoverflow.com/questions/158174/why-would-you-ever-implement-finalize
    //
//    protected void finalize() throws Throwable {
//        try {
//            this.ysparse_destroy(this.ysparse);
//        }
//        finally {
//            super.finalize();
//        }
//    }


    //------------------------
    // EVT
    //------------------------

    public boolean parseYs(byte[] src, byte[] arena, int[] evts) throws Exception
    {
        return parseYs("yamlscript", src, arena, evts);
    }

    public boolean parseYsDirect(ByteBuffer src, ByteBuffer arena, IntBuffer evt) throws Exception
    {
        return parseYsDirect("yamlscript", src, arena, evt);
    }

    public boolean parseYs(String filename, byte[] src, byte[] arena, int[] evts) throws Exception
    {
        long t = timingStart("ysparse");
        boolean fits_buffers = ysparse_parse(this.ysparse, filename,
                                             src, src.length,
                                             arena, arena.length,
                                             evts, evts.length);
        timingStop("ysparse", t, src.length);
        return fits_buffers;
    }

    public boolean parseYsDirect(String filename, ByteBuffer src, ByteBuffer arena, IntBuffer evt) throws Exception {
        if (! src.isDirect())
            throw new RuntimeException("src must be direct");
        if (! arena.isDirect())
            throw new RuntimeException("arena must be direct");
        if (! evt.isDirect())
            throw new RuntimeException("evt must be direct");
        // the byte order for src does not matter
        // but for evt it really does
        if (evt.order() != ByteOrder.nativeOrder())
            throw new RuntimeException("evt byte order must be native");
        long t = timingStart("ysparseBuf");
        evt.position(evt.capacity());
        boolean fits_buffers = ysparse_parse_buf(this.ysparse, filename,
                                                 src, src.position(),
                                                 arena, arena.position(),
                                                 evt, evt.capacity());
        if (fits_buffers)
            evt.position(ysparse_reqsize_evt(this.ysparse));
        timingStop("ysparseBuf", t, src.position());
        return fits_buffers;
    }

    /** Get the required size for the event output buffer, from the last parse call */
    public int reqsizeEvt() { return ysparse_reqsize_evt(this.ysparse); }
    /** Get the required size for the arena buffer, from the last parse call */
    public int reqsizeArena() { return ysparse_reqsize_arena(this.ysparse); }

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
