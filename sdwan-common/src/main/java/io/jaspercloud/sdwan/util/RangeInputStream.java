package io.jaspercloud.sdwan.util;

import java.io.IOException;
import java.io.InputStream;

public class RangeInputStream extends InputStream {

    private InputStream stream;
    private long size;

    public RangeInputStream(InputStream stream, long size) {
        this.stream = stream;
        this.size = size;
    }

    @Override
    public int read() throws IOException {
        if (size <= 0) {
            return -1;
        }
        int read = stream.read();
        size--;
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = stream.read(b, off, len);
        if (read > 0) {
            size -= read;
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
