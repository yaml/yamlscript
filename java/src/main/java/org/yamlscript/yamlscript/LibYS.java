// Copyright 2023-2025 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package org.yamlscript.yamlscript;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;

public class LibYS {
    public static String extension()
    {
        return Platform.isMac() ? "dylib" : "so";
    }

    public static char pathDelimiter()
    {
        return Platform.isWindows() ? '\\' : '/';
    }

    public static String filename()
    {
        return "libys." + extension() + '.' +
               YAMLScript.YAMLSCRIPT_VERSION;
    }

    public static String[] libraryPaths()
    {
        String envValue = System.getenv("LD_LIBRARY_PATH");
        if (envValue == null) return new String[0];

        return envValue.split(":");
    }

    public static String path() throws RuntimeException
    {
        String name = filename();
        String[] dirs = libraryPaths();

        String path = null;
        for (String dir : dirs) {
            path = dir + pathDelimiter() + name;
            if (new File(path).exists()) break;
        }
        if (path != null) return path;

        path = "/usr/local/lib" + pathDelimiter() + name;
        if (new File(path).exists()) return path;

        path = System.getenv("HOME") + pathDelimiter() +
               ".local/lib" + pathDelimiter() + name;
        if (new File(path).exists()) return path;

        throw new RuntimeException(
            "Shared library file " + name + " not found\n" +
            "Try: curl -sSL https://yamlscript.org/install | VERSION=" +
            YAMLScript.YAMLSCRIPT_VERSION + " LIB=1 bash\n" +
            "See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript"
        );
    }

    public static ILibYS load(String path)
    {
        return Native.load(path, ILibYS.class);
    }

    public static ILibYS load()
    {
        return load(path());
    }

    private static ILibYS INSTANCE = null;

    public static ILibYS library()
    {
        if (INSTANCE == null) INSTANCE = load();

        return INSTANCE;
    }
}
