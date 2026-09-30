package com.acme.orders.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Scratch file used while streaming large order exports.
 */
public class TempSpoolFile implements Closeable {

    private final File file;
    private final RandomAccessFile handle;

    public TempSpoolFile(String prefix) throws IOException {
        this.file = File.createTempFile(prefix, ".spool");
        this.handle = new RandomAccessFile(file, "rw");
    }

    public void write(String line) throws IOException {
        handle.writeBytes(line);
        handle.writeBytes("\n");
    }

    public File getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        handle.close();
    }

    /** Safety net: make sure the OS handle is released even if close() is missed. */
    @Override
    protected void finalize() throws Throwable {
        try {
            handle.close();
            file.delete();
        } finally {
            super.finalize();
        }
    }
}
