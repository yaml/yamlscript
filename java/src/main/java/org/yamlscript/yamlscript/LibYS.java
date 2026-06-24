// Copyright 2023-2026 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package org.yamlscript.yamlscript;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LibYS {
    public static String extension()
    {
        return extension(Platform.isWindows(), Platform.isMac());
    }

    static String extension(boolean isWindows, boolean isMac)
    {
        if (isWindows) return "dll";

        return isMac ? "dylib" : "so";
    }

    public static char pathDelimiter()
    {
        return File.separatorChar;
    }

    public static String filename()
    {
        return filename(Platform.isWindows(), Platform.isMac());
    }

    static String filename(boolean isWindows, boolean isMac)
    {
        String name = "libys." + extension(isWindows, isMac);

        return isWindows ? name : name + '.' + YAMLScript.YAMLSCRIPT_VERSION;
    }

    public static String[] libraryPaths()
    {
        List<String> paths = new ArrayList<String>();
        String javaPath = System.getProperty("java.library.path");

        addPaths(paths, javaPath);
        paths.add(".");
        if (Platform.isWindows()) {
            addPaths(paths, System.getenv("PATH"));
        } else if (Platform.isMac()) {
            addPaths(paths, System.getenv("DYLD_LIBRARY_PATH"));
        } else {
            addPaths(paths, System.getenv("LD_LIBRARY_PATH"));
        }

        return paths.toArray(new String[0]);
    }

    static void addPaths(List<String> paths, String value)
    {
        if (value == null || value.length() == 0) return;

        for (String path : value.split(File.pathSeparator)) {
            if (path.length() > 0) paths.add(path);
        }
    }

    static String findLibrary(String name, String[] dirs)
    {
        for (String dir : dirs) {
            String path = dir + pathDelimiter() + name;
            if (new File(path).exists()) return path;
        }

        return null;
    }

    public static String path() throws RuntimeException
    {
        String name = filename();
        String path = findLibrary(name, libraryPaths());
        if (path != null) return path;

        path = "/usr/local/lib" + pathDelimiter() + name;
        if (new File(path).exists()) return path;

        String home = System.getProperty("user.home");
        if (home != null) {
            path = home + pathDelimiter() + ".local/lib" +
                   pathDelimiter() + name;
            if (new File(path).exists()) return path;
        }

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
