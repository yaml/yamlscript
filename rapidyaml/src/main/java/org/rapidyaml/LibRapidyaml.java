// Copyright 2023-2024 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package org.rapidyaml;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;

public class LibRapidyaml {
    public static String extension()
    {
        return Platform.isMac() ? "so" : "so";
    }

    public static char pathDelimiter()
    {
        return Platform.isWindows() ? '\\' : '/';
    }

    public static String filename()
    {
        return "librapidyaml." + extension() + '.' +
               Rapidyaml.RAPIDYAML_VERSION;
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
	System.out.println(path);
        if (path != null) return path;

        path = "/usr/local/lib" + pathDelimiter() + name;
	System.out.println(path);
        if (new File(path).exists()) return path;

        path = System.getenv("HOME") + pathDelimiter() +
               ".local/lib" + pathDelimiter() + name;
	System.out.println(path);
        if (new File(path).exists()) return path;

        throw new RuntimeException(
            "Shared library file " + name + " not found\n"
        );
    }

    public static ILibRapidyaml load(String path)
    {
        return Native.load(path, ILibRapidyaml.class);
    }

    public static ILibRapidyaml load()
    {
        return load(path());
    }

    private static ILibRapidyaml INSTANCE = null;

    public static ILibRapidyaml library()
    {
        if (INSTANCE == null) INSTANCE = load();

        return INSTANCE;
    }
}
