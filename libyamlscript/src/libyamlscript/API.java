// Copyright 2023-2024 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package libyamlscript;

import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.type.CConst;

public final class API {
    @CEntryPoint(name = "print_hello")
    public static void printHello(
        @CEntryPoint.IsolateThreadContext long isolateId
    ) {
        if (System.getenv("LIBYAMLSCRIPT_DEBUG") != null) {
            System.err.println("API - in printHello");
        }
        libyamlscript.core.printHello();
    }

    @CEntryPoint(name = "triple_num")
    public static int tripleNum(
        @CEntryPoint.IsolateThreadContext long isolateId,
        int num
    ) {
        if (System.getenv("LIBYAMLSCRIPT_DEBUG") != null) {
            System.err.println("API - in tripleNum:" + num);
        }
        return libyamlscript.core.tripleNum(num);
    }

    @CEntryPoint(name = "load_ys_to_json")
    public static @CConst CCharPointer loadYsToJson(
        @CEntryPoint.IsolateThreadContext long isolateId,
        @CConst CCharPointer s
    ) {
        if (System.getenv("LIBYAMLSCRIPT_DEBUG") != null) {
            System.err.println("API - called loadYsToJson");
        }
        String ys = CTypeConversion.toJavaString(s);
        if (System.getenv("LIBYAMLSCRIPT_DEBUG") != null) {
            System.err.println("API - java input string: " + ys);
        }
        String json = libyamlscript.core.loadYsToJson(ys);
        if (System.getenv("LIBYAMLSCRIPT_DEBUG") != null) {
            System.err.println("API - java response string: " + json);
        }
        try (CTypeConversion.CCharPointerHolder holder =
                CTypeConversion.toCString(json)
        ) {
            CCharPointer value = holder.get();
            return value;
        }
    }
}
