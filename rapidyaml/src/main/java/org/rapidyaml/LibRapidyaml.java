// Copyright 2023-2024 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package org.rapidyaml;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.io.File;
import java.util.*;

public class LibRapidyaml {
    public static String ext()
    {
        return Platform.isMac() ? "dylib" : "so";
    }

    public static char slash()
    {
        return Platform.isWindows() ? '\\' : '/';
    }

    public static boolean isNativeImage()
    {
        return
            System.getProperty("java.vm.name") == "Substrate VM" &&
            System.getProperty("org.graalvm.nativeimage.imagecode") ==
                "runtime";
    }

    public static String getLibraryName()
    {
        return "librapidyaml." + ext() + '.' +
               Rapidyaml.RAPIDYAML_VERSION;
    }

    public static String[] libraryPaths()
    {
        String envValue = System.getenv("LD_LIBRARY_PATH");
        if (envValue == null) return new String[0];

        return envValue.split(":");
    }

    public static String findLibraryPath() throws RuntimeException
    {
        String libName = getLibraryName();
        String[] paths = libraryPaths();
        char slash = slash();

        String dirPath = null;
        String fullPath = null;
        for (String path : paths) {
            dirPath = path;
            fullPath = dirPath + slash + libName;
            if (new File(fullPath).exists()) return dirPath;
        }

        dirPath = "/usr/local/lib";
        fullPath = dirPath + slash + libName;
        if (new File(fullPath).exists()) return dirPath;

        dirPath = System.getenv("HOME") + slash + ".local" + slash + "lib";
        fullPath = dirPath + slash + libName;
        if (new File(fullPath).exists()) return dirPath;

        throw new RuntimeException(
            "Shared library file '" +
                libName +
                "'' not found by LibRapidyaml\n"
        );
    }

    public static ILibRapidyaml loadLibraryWithJNA()
    {
        if (isNativeImage())
        {
            System.setProperty(
                "java.class.path",
                "/home/ingy/src/yamlscript/rapidyaml/"
            );
            // String fullPath = findLibraryPath() + slash() + getLibraryName();
            return Native.load(
                "rapidyaml",
                // "librapidyaml.so",
                // fullPath,
                ILibRapidyaml.class,
                Collections.emptyMap()
            );
        }

        // Not a native image; Just in JVM
        String fullPath = findLibraryPath() + slash() + getLibraryName();
        System.out.println("Loading " + fullPath);
        return Native.load(fullPath, ILibRapidyaml.class);
    }

    private static ILibRapidyaml INSTANCE = null;

    public static ILibRapidyaml library()
    {
        if (INSTANCE == null) INSTANCE = loadLibraryWithJNA();

        return INSTANCE;
    }
}
