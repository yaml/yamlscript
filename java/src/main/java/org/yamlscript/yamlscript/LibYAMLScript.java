package org.yamlscript.yamlscript;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;

public class LibYAMLScript {
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
        return "libyamlscript." + extension() + '.' +
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
            "Try: curl -sSL yamlscript.org/install | VERSION=" +
            YAMLScript.YAMLSCRIPT_VERSION + " LIB=1 bash\n" +
            "See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript"
        );
    }

    public static ILibYAMLScript load(String path)
    {
        return Native.load(path, ILibYAMLScript.class);
    }

    public static ILibYAMLScript load()
    {
        return load(path());
    }

    private static ILibYAMLScript INSTANCE = null;

    public static ILibYAMLScript library()
    {
        if (INSTANCE == null) INSTANCE = load();

        return INSTANCE;
    }
}
