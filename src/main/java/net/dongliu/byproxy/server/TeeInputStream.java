package net.dongliu.byproxy.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * InputStream that write all read data to output stream.
 * Call skip(long) or mark(int)reset() on the stream will result on some bytes from the input stream being skipped or duplicated in the output stream.
 *
 * @author Liu Dong
 */
class TeeInputStream extends InputStream {
    private final InputStream in;
    private final OutputStream out;

    public TeeInputStream(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            out.write(b);
        }
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read != -1) {
            out.write(b, off, read);
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
}
