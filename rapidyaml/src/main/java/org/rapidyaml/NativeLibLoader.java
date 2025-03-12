package org.rapidyaml;

import java.io.*;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardCopyOption;

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;
import java.text.MessageFormat;

public class NativeLibLoader {
    private static File tempdir;
    private static final String LOCK_EXT = ".lck";

    private NativeLibLoader() {}


    /**
     * Extracts and loads the specified library file to the target folder
     *
     * @param libFolderForCurrentOS Library path.
     * @param libraryFileName Library name.
     * @param targetFolder Target folder.
     * @return
     * -Dorg.sqlite.lib.path=.
     * -Dorg.sqlite.lib.name=sqlite_cryption_support.dll
     */

    public static boolean extractAndLoadLibraryFile(String libraryFileName)
        throws Exception
    {
        System.out.printf(">>> libraryFileName = %s\n", libraryFileName);
        String libFolderForCurrentOS = "";
        String targetFolder = "rapidyamllibloader";
        // String targetFolder = System.getProperty("java.io.tmpdir");

        String nativeLibraryFilePath =
            libFolderForCurrentOS + "/" + libraryFileName;
        System.out.printf(">>> FOO nativeLibraryFilePath = %s\n", nativeLibraryFilePath);
        // Include architecture name in temporary filename in order to avoid
        // conflicts when multiple JVMs with different architectures running at
        // the same time
        String uuid = UUID.randomUUID().toString();
        String extractedLibFileName = String.format(
            "librapidyaml-%s-%s-%s",
            "0.8.0",
            uuid,
            libraryFileName);
        String extractedLckFileName = extractedLibFileName + LOCK_EXT;

        Path extractedLibFile = Paths.get(targetFolder, extractedLibFileName);
        Path extractedLckFile = Paths.get(targetFolder, extractedLckFileName);

        try {
            // Extract a native library file into the target directory
            try (InputStream reader = getResourceAsStream(nativeLibraryFilePath)) {
                if (Files.notExists(extractedLckFile)) {
                    Files.createFile(extractedLckFile);
                }

                Files.copy(reader, extractedLibFile, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                // Delete the extracted lib file on JVM exit.
                extractedLibFile.toFile().deleteOnExit();
                extractedLckFile.toFile().deleteOnExit();
            }

            // Set executable (x) flag to enable Java to load the native library
            extractedLibFile.toFile().setReadable(true);
            extractedLibFile.toFile().setWritable(true, true);
            extractedLibFile.toFile().setExecutable(true);

            // Check whether the contents are properly copied from the resource folder
            {
                try (InputStream nativeIn = getResourceAsStream(nativeLibraryFilePath);
                        InputStream extractedLibIn = Files.newInputStream(extractedLibFile)) {
                    if (!contentsEquals(nativeIn, extractedLibIn)) {
                        throw new Exception(
                                String.format(
                                        "Failed to write a native library file at %s",
                                        extractedLibFile));
                    }
                }
            }
            return loadNativeLibrary(targetFolder, extractedLibFileName);
        } catch (IOException e) {
            // logger.error(() -> "Unexpected IOException", e);
            return false;
        }
    }

    // Replacement of java.lang.Class#getResourceAsStream(String) to disable sharing the resource
    // stream
    // in multiple class loaders and specifically to avoid
    // https://bugs.openjdk.java.net/browse/JDK-8205976
    private static InputStream getResourceAsStream(String name) {
        // Remove leading '/' since all our resource paths include a leading directory
        // See:
        // https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/Class.java#L3054
        String resolvedName = name.substring(1);
        ClassLoader cl = NativeLibLoader.class.getClassLoader();
        URL url = cl.getResource(resolvedName);
        if (url == null) {
            return null;
        }
        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException e) {
            // logger.error(() -> "Could not connect", e);
            return null;
        }
    }


    /**
     * Loads native library using the given path and name of the library.
     *
     * @param path Path of the native library.
     * @param name Name of the native library.
     * @return True for successfully loading; false otherwise.
     */
    private static boolean loadNativeLibrary(String path, String name) {
        File libPath = new File(path, name);
        if (libPath.exists()) {

            try {
                System.load(new File(path, name).getAbsolutePath());
                return true;
            } catch (UnsatisfiedLinkError e) {

//               logger.error(
//                       () ->
//                               MessageFormat.format(
//                                       "Failed to load native library: {0}. osinfo: {1}",
//                                       name, OSInfo.getNativeLibFolderPathForCurrentOS()),
//                       e);
                return false;
            }

        } else {
            return false;
        }
    }

    private static boolean contentsEquals(InputStream in1, InputStream in2) throws IOException {
        if (!(in1 instanceof BufferedInputStream)) {
            in1 = new BufferedInputStream(in1);
        }
        if (!(in2 instanceof BufferedInputStream)) {
            in2 = new BufferedInputStream(in2);
        }

        int ch = in1.read();
        while (ch != -1) {
            int ch2 = in2.read();
            if (ch != ch2) {
                return false;
            }
            ch = in1.read();
        }
        int ch2 = in2.read();
        return ch2 == -1;
    }


    public static void loadLibraryFromJar(String path) throws IOException {
//       if (null == path || !path.startsWith("/")) {
//           throw new IllegalArgumentException(
//               "The path has to be absolute (start with '/').");
//       }

//       String[] parts = path.split("/");
//       String filename = (parts.length > 1) ? parts[parts.length - 1] : null;
//       System.out.printf("jar path file = %s\n", filename);

        if (tempdir == null) {
            tempdir = createTempDirectory("rapidyamllibloader");
            // tempdir.deleteOnExit();
        }

        System.out.printf("TEMPDIR = %s\n", tempdir.getAbsolutePath());
        System.out.printf("%s\n", System.getProperty("java.library.path"));
        System.setProperty("java.library.path", tempdir.getAbsolutePath());
        System.out.printf("%s\n", System.getProperty("java.library.path"));


//       File temp = new File(tempdir, filename);
        File temp = new File(tempdir, path);

        try (InputStream is = NativeLibLoader.class.getResourceAsStream(path)) {
            System.out.printf("InputStream: >>>%s<<<\n", is);
            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            is.close();
        }
        catch (IOException e) {
            // temp.delete();
            throw e;
        }
        catch (NullPointerException e) {
            // temp.delete();
            throw new FileNotFoundException(
                "File '" + path + "' was not found inside JAR.");
        }
        finally {
        }

        try {
            System.load(temp.getAbsolutePath());
        }
        finally {
            if (isPosixCompliant()) {
                // temp.delete();
            }
            else {
                // temp.deleteOnExit();
            }
        }
    }

    private static boolean isPosixCompliant() {
        try {
            return FileSystems.getDefault()
                    .supportedFileAttributeViews()
                    .contains("posix");
        }
        catch (FileSystemNotFoundException
                | ProviderNotFoundException
                | SecurityException e) {
            return false;
        }
    }

    private static File createTempDirectory(String prefix) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File generatedDir = new File(tempDir, prefix + System.nanoTime());

        if (!generatedDir.mkdir())
            throw new IOException(
                "Failed to create temp directory " + generatedDir.getName());

        return generatedDir;
    }
}
