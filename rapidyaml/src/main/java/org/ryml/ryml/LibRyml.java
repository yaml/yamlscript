// Copyright 2023-2024 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package org.ryml.ryml;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;

public class LibRyml {
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
        return "libryml." + extension() + '.' +
               Ryml.RYML_VERSION;
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
            "Shared library file " + name + " not found\n"
        );
    }

    public static ILibRyml load(String path)
    {
        return Native.load(path, ILibRyml.class);
    }

    public static ILibRyml load()
    {
        return load(path());
    }

    private static ILibRyml INSTANCE = null;

    public static ILibRyml library()
    {
        if (INSTANCE == null) INSTANCE = load();

        return INSTANCE;
    }
}
