package org.rapidyaml;

import java.io.*;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardCopyOption;

public class NativeLibLoader {
    private static File tempdir;

    private NativeLibLoader() {}

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

//       File temp = new File(tempdir, filename);
        File temp = new File(tempdir, path);

        try (InputStream is = NativeLibLoader.class.getResourceAsStream(path)) {
            System.out.printf("InputStream: >>>%s<<<\n", is);
            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
