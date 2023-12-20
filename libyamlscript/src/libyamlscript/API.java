// Copyright 2023 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package libyamlscript;

import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.type.CConst;

public final class API {
    @CEntryPoint(name = "eval_ys_to_json")
    public static @CConst CCharPointer evalYsToJson(
        @CEntryPoint.IsolateThreadContext long isolateId,
        @CConst CCharPointer s
    ) {
        String ys = CTypeConversion.toJavaString(s);
        String json = libyamlscript.core.evalYsToJson(ys);
        try (CTypeConversion.CCharPointerHolder holder = CTypeConversion.toCString(json)) {
            CCharPointer value = holder.get();
            return value;
        }
    }

    @CEntryPoint(name = "compile_ys_to_clj")
    public static @CConst CCharPointer compileYsToClj(
        @CEntryPoint.IsolateThreadContext long isolateId,
        @CConst CCharPointer s
    ) {
        String ys = CTypeConversion.toJavaString(s);
        String json = libyamlscript.core.compileYsToClj(ys);
        try (CTypeConversion.CCharPointerHolder holder = CTypeConversion.toCString(json)) {
            CCharPointer value = holder.get();
            return value;
        }
    }
}
