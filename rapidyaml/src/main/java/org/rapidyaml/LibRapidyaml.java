// Copyright 2023-2024 Ingy dot Net
// This code is licensed under MIT license (See License for details)

package org.rapidyaml;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.NativeLibrary;

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

    public static void setPropertyTrueIfEnv(String key, String env)
    {
        if (System.getenv(env) != null) System.setProperty(key, "true");
    }

    public static void setPropertyIfEnv(String key, String env)
    {
        String val = System.getenv(env);
        if (val != null) System.setProperty(key, val);
    }

    public static void setPropertiesFromEnv()
    {
        setPropertyIfEnv("java.class.path", "YS_JAVA_CLASS_PATH");
        setPropertyIfEnv("jna.library.path", "YS_JNA_LIBRARY_PATH");
        setPropertyTrueIfEnv("jna.nosys", "YS_JNA_NOSYS");
        setPropertyTrueIfEnv("jna.noclasspath", "YS_JNA_NOCLASSPATH");
        setPropertyTrueIfEnv("jna.nounpack", "YS_JNA_NOUNPACK");
        setPropertyTrueIfEnv("jna.debug_load", "YS_JNA_DEBUG_LOAD");
        setPropertyTrueIfEnv("jna.debug_load.jna", "YS_JNA_DEBUG_LOAD_JNA");
    }

    public static void printSystemProperties()
    {
        Properties props = System.getProperties();
        List<String> keys = new ArrayList(props.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            System.out.println(key + " = '" + props.getProperty(key) + "'");
        }
    }

    public static ILibRapidyaml loadLibraryWithJNA()
    {
        if (false && isNativeImage())
        {
            setPropertiesFromEnv();
            if (System.getenv("YS_SHOW_PROPS") != null)
                printSystemProperties();
            String nativeName = "rapidyaml";
            String nativeNameEnv = System.getenv("YS_NATIVE_NAME");
            if (nativeNameEnv != null) nativeName = nativeNameEnv;
            return Native.load(
                nativeName,
                ILibRapidyaml.class,
                Collections.emptyMap()
            );
        }

        if (isNativeImage())
        {
            String libPath = "/home/ingy/src/yamlscript/rapidyaml/native";
            String nativeName = "rapidyaml";
            String nativeNameEnv = System.getenv("YS_NATIVE_NAME");
            if (nativeNameEnv != null) nativeName = nativeNameEnv;
            // System.setProperty("jna.library.path", libPath);
            // System.setProperty("java.class.path", libPath);
            // System.setProperty("jna.prefix", "");
            NativeLibrary lib = NativeLibrary.getInstance(nativeName);
            System.out.println("================== Loaded: " + lib);
            return null;
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
