package net.dongliu.byproxy.parser;

import com.google.common.io.ByteStreams;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input streams, that delegate to another input stream, and have helper methods.
 *
 * @author Liu Dong
 */
public class RichInputStream extends InputStream {

    protected InputStream in;

    // only for mark & reset
    private byte[] buffer;
    private int pos = 0;
    private int count = 0;
    private boolean mark;

    public RichInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * Read exactly size bytes
     */
    public byte[] readExact(int size) throws IOException {
        byte[] buffer = new byte[size];
        ByteStreams.readFully(this, buffer);
        return buffer;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (mark) {
            int read = in.read(b, off, len);
            if (read <= 0) {
                return read;
            }
            System.arraycopy(b, off, buffer, count, read);
            count += read;
            return read;
        }
        if (pos < count) {
            int toRead = Math.min(len, count - pos);
            System.arraycopy(buffer, pos, b, off, toRead);
            pos += toRead;
            return toRead;
        }
        return in.read(b, off, len);
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        if (mark) {
            int toSkip = (int) Math.min(buffer.length - count, n);
            int read = in.read(buffer, count, toSkip);
            if (read == -1) {
                return 0;
            }
            count += read;
            return read;
        }
        if (pos < count) {
            int toSkip = (int) Math.min(n, count - pos);
            pos += toSkip;
            return toSkip;
        }
        return in.skip(n);
    }

    @Override
    public synchronized int available() throws IOException {
        if (!mark && pos < count) {
            return count - pos;
        }
        return in.available();
    }

    @Override
    public synchronized void close() throws IOException {
        buffer = null;
        pos = count = 0;
        mark = false;
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        mark = true;
        pos = count = 0;
        if (buffer == null) {
            buffer = new byte[readlimit];
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        mark = false;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized int read() throws IOException {
        if (mark) {
            int b = in.read();
            if (b == -1) {
                return b;
            }
            buffer[count++] = (byte) b;
            return b;
        }

        if (pos < count) {
            return Byte.toUnsignedInt(buffer[pos++]);
        }
        return in.read();
    }

    public synchronized int readUnsignedInt8() throws IOException {
        int b = read();
        if (b == -1) {
            return -1;
        }
        return b & 0xff;
    }

    public synchronized int readUnsignedInt16() throws IOException {
        int b = read();
        if (b == -1) {
            return -1;
        }
        return ((b & 0xff) << 8) | (read() & 0xff);
    }


    public void enableBuffered() {
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
    }

    /**
     * Read ascii line, separated by '\r\n'
     */
    @Nullable
    public synchronized String readLine() throws IOException {
        return Lines.readLine(this);
    }
}
