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

        InputStream inputStream =
            classLoader.getResourceAsStream(libraryName);

        if (inputStream == null)
            throw new IOException(
                "Failed to load library resource '" +
                libraryName + "' from NativeLibLoader");

System.out.println("inputStream: " + inputStream);

        File tempDir = createTempDir();
        // tempDir.deleteOnExit();
        File tempFile = new File(tempDir, libraryName);
System.out.println("tempFile: " + tempFile);

        Files.copy(
            inputStream,
            tempFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING);

        inputStream.close();

        try {
            System.load(tempFile.getAbsolutePath());
        }
        finally {
            // tempFile.delete();
        }

System.out.println("Loaded library from temp file");
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
