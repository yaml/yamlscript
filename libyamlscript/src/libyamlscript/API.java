// Copyright 2023-2024 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package libyamlscript;

import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.type.CConst;

public final class API {
    @CEntryPoint(name = "load_ys_to_json")
    public static @CConst CCharPointer loadYsToJson(
        @CEntryPoint.IsolateThreadContext long isolateId,
        @CConst CCharPointer s
    ) {
        String ys = CTypeConversion.toJavaString(s);
        if (System.getenv("LIBYAMLSCRIPT_DEBUG") != null) {
            System.err.println("java input string: " + ys.toString());
        }
        String json = libyamlscript.core.loadYsToJson(ys);
        if (System.getenv("LIBYAMLSCRIPT_DEBUG") != null) {
            System.err.println("java response string: " + json.toString());
        }
        try (CTypeConversion.CCharPointerHolder holder =
                CTypeConversion.toCString(json)
        ) {
            CCharPointer value = holder.get();
            return value;
        }
    }
}
