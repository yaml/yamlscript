package cmp;

import org.rapidyaml.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// https://stackoverflow.com/questions/804466/how-do-i-create-executable-java-program
public class CmpEdnEvt
{
    public static void main(String[] args) throws Throwable
    {
        Rapidyaml rapidyaml = new Rapidyaml();
        int evtSize = 10000000;
        compareEdnEvt(evtSize, rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/appveyor.yml");
        compareEdnEvt(evtSize, rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/compile_commands.json");
        compareEdnEvt(evtSize, rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_seqs_flow_outer1000_inner100.yml");
        compareEdnEvt(evtSize, rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_maps_flow_outer1000_inner100.yml");
        compareEdnEvt(evtSize, rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_seqs_flow_outer1000_inner1000.yml");
        compareEdnEvt(evtSize, rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_maps_flow_outer1000_inner1000.yml");
        compareEdnEvt(evtSize, rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_seqs_flow_outer1000_inner1000_json.json");
        compareEdnEvt(evtSize, rapidyaml, "/home/jpmag/proj/rapidyaml/bm/cases/style_maps_flow_outer1000_inner1000_json.yml");
    }

    public static void compareEdnEvt(int evtSize, Rapidyaml rapidyaml, String path) throws Throwable
    {
        String ys = java.nio.file.Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        int[] evt = new int[evtSize];
        //
        long t0 = System.nanoTime();
        byte[] ysBytes = ys.getBytes(StandardCharsets.UTF_8);
        long tys2Bytes = System.nanoTime() - t0;
        System.out.printf("-----\n");
        System.out.printf("%s\n", path);
        System.out.printf("     ys.length=%d\n", ys.length());
        System.out.printf("     ys2bytes=%fms\n", (double)tys2Bytes / 1.e6);
        //
        System.out.printf("     edn...\n");
        t0 = System.nanoTime();
        String edn = rapidyaml.parseYsToEdn(ys);
        long tEdn = System.nanoTime() - t0;
        System.out.printf("     edn=%.6fms, length=%d -> %dB @%.3fMB/s\n", (double)tEdn / 1.e6, edn.length(), edn.length(), (double)ysBytes.length / tEdn * 1.e3);
        //
        System.out.printf("     evt...\n");
        t0 = System.nanoTime();
        int numEvts = rapidyaml.parseYsToEvt(ysBytes, evt);
        long tEvt = System.nanoTime() - t0;
        System.out.printf("     evt=%.6fms, length=%d -> %dB @%.3fMB/s\n", (double)tEvt / 1.e6, numEvts, 4*numEvts, (double)ysBytes.length / tEvt * 1.e3);
        //
    }
}
