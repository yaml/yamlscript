package org.rapidyaml;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

class NativeLibLoader {
    public static void loadLibraryFromResource(String libraryName)
        throws IOException
    {
System.out.println("libraryName: " + libraryName);
        ClassLoader classLoader =
            Thread.currentThread().getContextClassLoader();
            // Rapidyaml.class.getClassLoader();
System.out.println("classLoader: " + classLoader);

        InputStream inputStream =
            classLoader.getResourceAsStream(libraryName);
System.out.println("inputStream: " + inputStream);

        File tempDir = createTempDir();
        // tempDir.deleteOnExit();
        File tempFile = new File(tempDir, libraryName);
System.out.println("tempFile: " + tempFile);

        Files.copy(
            inputStream,
            tempFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING);
System.out.println("Copied library to temp file");

        inputStream.close();
System.out.println("Closed input stream");

System.out.println("Loading library from temp file");
        try { System.load(tempFile.getAbsolutePath());
System.out.println("Loaded library from temp file");
        }
        finally {
            // tempFile.delete();
        }
    }

    private static File createTempDir() throws IOException {
        String tempBase = System.getProperty("java.io.tmpdir");
        File tempDir = new File(tempBase, "" + System.nanoTime());
        if (! tempDir.mkdir())
            throw new IOException(
                "Failed to create temp directory " +
                tempDir.getName());
        return tempDir;
    }
}
