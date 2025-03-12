/* Links to figure out how to make loading shared libraries work with GraalVM:

 * https://www.adamh.cz/blog/2012/12/how-to-load-native-jni-library-from-jar/
   * https://github.com/adamheinrich/native-utils/blob/master/src/main/java/cz/adamh/utils/NativeUtils.java

 * https://github.com/Willena/sqlite-jdbc-crypt/tree/master?tab=readme-ov-file#graalvm-native-image-support
   * https://github.com/Willena/sqlite-jdbc-crypt/issues/61
   * https://github.com/Willena/sqlite-jdbc-crypt/pull/62/files
   * https://github.com/oracle/graal/issues/4579
   * https://github.com/Willena/sqlite-jdbc-crypt/blob/master/src/main/java/org/sqlite/SQLiteJDBCLoader.java

 * https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
 * https://github.com/oracle/graal/blob/graal-22.3.3/docs/reference-manual/native-image/Resources.md
 * https://stackoverflow.com/questions/1983839/determine-which-jar-file-a-class-is-from
 * https://stackoverflow.com/questions/1429172/how-to-list-the-files-inside-a-jar-file
 * https://docs.oracle.com/javase/8/docs/api/java/lang/ClassLoader.html#getSystemResources-java.lang.String-
 */
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
 * Interface with the shared librapidyaml library
 */
public class Rapidyaml
{

    //------------------------
    // JNI
    //------------------------

    public static String RAPIDYAML_NAME = "rapidyaml";
    public static String RAPIDYAML_VERSION = "0.8.0";
    public static String RAPIDYAML_LIBNAME =
        String.format("%s.%s", RAPIDYAML_NAME, RAPIDYAML_VERSION);

    private native long ysparse_init();
    private native void ysparse_destroy(long ysparse);
    private native void ysparse_timing_set(boolean yes);
    private native int ysparse_parse(long ysparse, String filename,
                                     byte[] ys, int ys_length,
                                     int[] evt, int evt_length);
    private native int ysparse_parse_buf(long ysparse, String filename,
                                         ByteBuffer ys, int ys_length,
                                         IntBuffer evt, int evt_length);

    private final long ysparse;


    //------------------------
    // CTOR/DTOR
    //------------------------

    public Rapidyaml() throws Exception, IOException
    {
        printJarInfo();
        System.out.printf("RAPIDYAML_LIBNAME : >>>%s<<<\n", RAPIDYAML_LIBNAME);

        System.loadLibrary(RAPIDYAML_LIBNAME);

/*
        if (System.getenv("YS_RAPIDYAML_MAVEN_TEST") != null) {
            String library_name = "rapidyaml";
            System.loadLibrary(library_name);
        }
        else {
            //String library_name = "rapidyaml." + RAPIDYAML_VERSION;
            //NativeLibLoader.loadLibraryFromJar(library_name);

            //System.out.printf("LOADING library from jar...\n");
            //NativeLibLoader.loadLibraryFromJar("/librapidyaml.0.8.0.so");

            System.loadLibrary(RAPIDYAML_LIBNAME);
        }
*/


        this.ysparse = this.ysparse_init();
        timingEnabled(false);
    }

    public String printJarInfo() {
        Class class_ = Rapidyaml.class;

        System.out.printf("class name: >>>%s<<<\n", class_.getName());

        URL location = class_.getResource(
            '/' + class_.getName().replace('.', '/') + ".class");
        System.out.printf("location: >>>%s<<<\n", location);

        System.out.printf("jar: >>>%s<<<\n",
            class_.getProtectionDomain().getCodeSource().getLocation());

        return null;
    }

    // Likely bad idea to implement finalize:
    //
    // https://stackoverflow.com/questions/158174/why-would-you-ever-implement-finalize
    //
    protected void finalize() throws Throwable
    {
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
        long t = timingStart("ysparse");
        int required_size = ysparse_parse(this.ysparse, filename, src, src.length, evts, evts.length);
        timingStop("ysparse", t, src.length);
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
        long t = timingStart("ysparseBuf");
        evt.position(evt.capacity());
        int reqsize = ysparse_parse_buf(this.ysparse, filename, src, src.position(), evt, evt.capacity());
        if(reqsize <= evt.capacity()) {
            evt.position(reqsize);
        }
        timingStop("ysparseBuf", t, src.position());
        return reqsize;
    }

    public static IntBuffer mkIntBuffer(int numInts)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(/*numBytes*/4 * numInts);
        // !!! need to explicitly set the byte order to the native order
        return bb.order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    //------------------------
    // TIME
    //------------------------

    private boolean showTiming = true;

    public void timingEnabled(boolean yes)
    {
        showTiming = yes;
        ysparse_timing_set(yes);
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
