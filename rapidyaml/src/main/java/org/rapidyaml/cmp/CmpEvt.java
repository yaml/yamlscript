package cmp;

import org.rapidyaml.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

// https://stackoverflow.com/questions/804466/how-do-i-create-executable-java-program
public class CmpEvt
{
    public static void main(String[] args) throws Exception
    {
        Rapidyaml rapidyaml = new Rapidyaml();
        rapidyaml.timingEnabled(true);
        compareEvt(rapidyaml, "./yamllm.ys");
        compareEvt(rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/appveyor.yml");
        compareEvt(rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/compile_commands.json");
        compareEvt(rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_seqs_flow_outer1000_inner100.yml");
        compareEvt(rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_maps_flow_outer1000_inner100.yml");
        compareEvt(rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_seqs_flow_outer1000_inner1000.yml");
        compareEvt(rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_maps_flow_outer1000_inner1000.yml");
        compareEvt(rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_seqs_flow_outer1000_inner1000_json.json");
        compareEvt(rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_maps_flow_outer1000_inner1000_json.yml");
    }

    public static void compareEvt(Rapidyaml rapidyaml, String path) throws Exception
    {
        String ys_ = java.nio.file.Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        byte[] ys = ys_.getBytes(StandardCharsets.UTF_8);
        byte[] ysarr = new byte[ys.length];
        ByteBuffer ysbuf = ByteBuffer.allocateDirect(ys.length);
        //
        System.out.printf("-----\n");
        System.out.printf("%s\n", path);
        System.out.printf("     ys.length=%d\n", ys.length);
        //
        long t = timingStart("evt");
        int[] evtarr = callEvt(rapidyaml, ys, ysarr);
        timingStop("evt", t, ys.length);
        //
        t = timingStart("evtBuf");
        IntBuffer evtbuf = callEvtBuf(rapidyaml, ys, ysbuf);
        timingStop("evtBuf", t, ys.length);
    }

    static int[] callEvt(Rapidyaml rapidyaml, byte[] src, byte[] srcbuf) throws Exception
    {
        System.arraycopy(src, 0, srcbuf, 0, src.length);
        int[] evt = new int[10000000];
        int reqsize = rapidyaml.parseYsToEvt(srcbuf, evt);
        if(reqsize > evt.length) {
            evt = new int[reqsize];
            System.arraycopy(src, 0, srcbuf, 0, src.length);
            int reqsize2 = rapidyaml.parseYsToEvt(srcbuf, evt);
            if(reqsize2 != reqsize) {
                throw new RuntimeException("reqsize");
            }
            return evt;
        }
        int[] ret = new int[reqsize];
        System.arraycopy(evt, 0, ret, 0, reqsize);
        return ret;
    }

    static IntBuffer callEvtBuf(Rapidyaml rapidyaml, byte[] src, ByteBuffer srcbuf) throws Exception
    {
        srcbuf.position(0);
        srcbuf.put(src);
        IntBuffer evt = Rapidyaml.mkIntBuffer(10000000);
        int reqsize = rapidyaml.parseYsToEvtBuf(srcbuf, evt);
        if(reqsize > evt.capacity()) {
            evt = Rapidyaml.mkIntBuffer(reqsize);
            srcbuf.position(0);
            srcbuf.put(src);
            int reqsize2 = rapidyaml.parseYsToEvtBuf(srcbuf, evt);
            if(reqsize2 != reqsize) {
                throw new RuntimeException("reqsize");
            }
        }
        evt.position(reqsize);
        return evt;
    }

    static private long timingStart(String name)
    {
        System.out.printf("     call:%s...\n", name);
        return System.nanoTime();
    }
    static private void timingStop(String name, long t)
    {
        t = System.nanoTime() - t;
        System.out.printf("     call:%s: %.6fms\n", name, (float)t/1.e6f);
    }
    static private void timingStop(String name, long t, int numBytes)
    {
        t = System.nanoTime() - t;
        float dt = (float)t;
        float fb = (float)numBytes;
        System.out.printf("     call:%s: %.6fms  %.3fMB/s  %dB\n", name, dt/1.e6f, 1.e3f*fb/dt, numBytes);
    }
}
