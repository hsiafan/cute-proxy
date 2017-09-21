package net.dongliu.byproxy.server;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Liu Dong
 */
class WrappedInputStream extends InputStream {
    private final InputStream inputStream;
    private final byte[] heading;
    private int remains;

    public WrappedInputStream(InputStream inputStream, byte[] heading) {
        this.inputStream = inputStream;
        this.heading = heading;
        this.remains = heading.length;
    }

    @Override
    public synchronized int read() throws IOException {
        if (remains > 0) {
            int read = 0xff & heading[heading.length - remains];
            remains--;
            return read;
        }
        return inputStream.read();
    }

    @Override
    public synchronized int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (remains > 0) {
            int read = Math.min(remains, len);
            System.arraycopy(heading, heading.length - remains, b, off, read);
            remains -= read;
            return read;
        }
        return inputStream.read(b, off, len);
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        if (remains > 0) {
            if (remains >= n) {
                remains -= n;
                return n;
            } else {
                return remains + inputStream.skip(n - remains);
            }
        }
        return inputStream.skip(n);
    }

    @Override
    public synchronized int available() throws IOException {
        return remains + inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

}
