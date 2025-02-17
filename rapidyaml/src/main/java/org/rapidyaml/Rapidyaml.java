package org.rapidyaml;

import org.rapidyaml.YamlParseErrorException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;

/**
 * Interface with the shared librapidyaml library
 */
public class Rapidyaml
{
    public static String RAPIDYAML_VERSION = "0.8.0";

    private native long ys2evt_init();
    private native long ys2edn_init();

    private native void ys2evt_destroy(long ryml2evt);
    private native void ys2edn_destroy(long ryml2edn);

    private native int ys2evt_parse(long ryml2evt, String filename,
                                    byte[] ys, int ys_length,
                                    int[] evt, int evt_length);
    private native int ys2evt_parse_buf(long ryml2evt, String filename,
                                        ByteBuffer ys, int ys_length,
                                        IntBuffer evt, int evt_length);
    private native int ys2edn_parse(long ryml2edn, String filename,
                                    byte[] ys, int ys_length,
                                    byte[] edn, int edn_length);
    private native int ys2edn_parse_buf(long ryml2edn, String filename,
                                        ByteBuffer ys, int ys_length,
                                        ByteBuffer edn, int edn_length);

    private final long ryml2edn;
    private final long ryml2evt;

    public Rapidyaml()
    {
        String library_name = "rapidyaml"; // ." + RAPIDYAML_VERSION;
        System.loadLibrary(library_name);
        this.ryml2edn = this.ys2edn_init();
        this.ryml2evt = this.ys2evt_init();
        // TODO: receive this argument as ctor parameter
        timingEnabled(System.getenv("YS_RYML_TIMER") != null);
    }

    // Likely bad idea to implement finalize:
    //
    // https://stackoverflow.com/questions/158174/why-would-you-ever-implement-finalize
    //
    protected void finalize() throws Throwable
    {
        try {
            this.ys2edn_destroy(this.ryml2edn);
            this.ys2evt_destroy(this.ryml2evt);
        }
        finally {
            super.finalize();
        }
    }


    //------------------------
    // EVT
    //------------------------

    public int parseYsToEvt(byte[] src, int[] evts) throws Exception
    {
        return parseYsToEvt("yamlscript", src, evts);
    }

    public int parseYsToEvtBuf(ByteBuffer src, IntBuffer evt) throws Exception
    {
        return parseYsToEvtBuf("yamlscript", src, evt);
    }

    public int parseYsToEvt(String filename, byte[] src, int[] evts) throws Exception
    {
        long t = timingStart("ys2evtBuf");
        int required_size = ys2evt_parse(this.ryml2evt, filename, src, src.length, evts, evts.length);
        timingStop("ys2evt", t, src.length);
        return required_size;
    }

    public int parseYsToEvtBuf(String filename, ByteBuffer src, IntBuffer evt) throws Exception
    {
        if(!src.isDirect())
            throw new RuntimeException("src must be direct");
        if(!evt.isDirect())
            throw new RuntimeException("evt must be direct");
        // the byte order for src does not matter
        // but for evt it really does
        if(evt.order() != ByteOrder.nativeOrder())
            throw new RuntimeException("evt byte order must be native");
        long t = timingStart("ys2evtBuf");
        evt.position(evt.capacity());
        int reqsize = ys2evt_parse_buf(this.ryml2evt, filename, src, src.position(), evt, evt.capacity());
        if(reqsize <= evt.capacity()) {
            evt.position(reqsize);
        }
        timingStop("ys2evtBuf", t, src.position());
        return reqsize;
    }

    public static IntBuffer mkIntBuffer(int numInts)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(/*numBytes*/4 * numInts);
        // !!! need to explicitly set the byte order to the native order
        return bb.order(ByteOrder.nativeOrder()).asIntBuffer();
    }


    //------------------------
    // EDN
    //------------------------

    public int parseYsToEdn(byte[] src, byte[] edn) throws Exception
    {
        return parseYsToEdn("yamlscript", src, edn);
    }

    public int parseYsToEdnBuf(ByteBuffer src, ByteBuffer edn) throws Exception
    {
        return parseYsToEdnBuf("yamlscript", src, edn);
    }

    public int parseYsToEdn(String filename, byte[] src, byte[] edn) throws Exception
    {
        long t = timingStart("ys2edn");
        int ret = ys2edn_parse(this.ryml2edn, filename, src, src.length, edn, edn.length);
        timingStop("ys2edn", t, src.length);
        return ret;
    }

    public int parseYsToEdnBuf(String filename, ByteBuffer src, ByteBuffer edn) throws Exception
    {
        if(!src.isDirect())
            throw new RuntimeException("src must be direct");
        if(!edn.isDirect())
            throw new RuntimeException("edn must be direct");
        long t = timingStart("ys2ednBuf");
        edn.position(edn.capacity());
        int reqsize = ys2edn_parse_buf(this.ryml2edn, filename, src, src.position(), edn, edn.capacity());
        if(reqsize <= edn.capacity()) {
            edn.position(reqsize);
        }
        timingStop("ys2ednBuf", t, src.position());
        return reqsize;
    }


    //------------------------
    // TIME
    //------------------------

    private boolean showTiming = true;

    public void timingEnabled(boolean yes)
    {
        showTiming = yes;
    }

    private long timingStart(String name)
    {
        if(showTiming) {
            System.out.printf("     java:%s...\n", name);
            return System.nanoTime();
        }
        return 0;
    }

    private void timingStop(String name, long t)
    {
        if(showTiming) {
            t = System.nanoTime() - t;
            System.out.printf("     java:%s: %.6fms\n", name, (float)t/1.e6f);
        }
    }

    private void timingStop(String name, long t, int numBytes)
    {
        if(showTiming) {
            t = System.nanoTime() - t;
            float dt = (float)t;
            float fb = (float)numBytes;
            System.out.printf("     java:%s: %.6fms  %.3fMB/s  %dB\n", name, dt/1.e6f, 1.e3f*fb/dt, numBytes);
        }
    }
}
