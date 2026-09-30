package com.acme.orders.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Scratch file used while streaming large order exports. Use with try-with-resources.
 */
public class TempSpoolFile implements Closeable {

    private final File file;
    private final RandomAccessFile handle;

    public TempSpoolFile(String prefix) throws IOException {
        this.file = File.createTempFile(prefix, ".spool");
        this.handle = new RandomAccessFile(file, "rw");
    }

    public void write(String line) throws IOException {
        handle.write((line + "\n").getBytes(StandardCharsets.UTF_8));
    }

    public File getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        handle.close();
        Files.deleteIfExists(file.toPath());
    }
}
