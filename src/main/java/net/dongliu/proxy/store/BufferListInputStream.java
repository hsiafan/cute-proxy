package net.dongliu.proxy.store;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Wrap chunk list as InputStream.
 */
class BufferListInputStream extends InputStream {
    private final List<ByteBuffer> bufferList;
    private int index;

    BufferListInputStream(List<ByteBuffer> bufferList) {
        this.bufferList = bufferList;
    }

    @Override
    public synchronized int read() {
        ByteBuffer buffer = indexBuffer();
        if (buffer == null) {
            return -1;
        }
        return buffer.get() & 0xff;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        ByteBuffer buffer = indexBuffer();
        if (buffer == null) {
            return -1;
        }
        int toRead = Math.min(len, buffer.remaining());
        buffer.get(b, off, toRead);
        return toRead;
    }

    @Override
    public synchronized long skip(long n) {
        if (n <= 0) {
            return 0;
        }
        ByteBuffer buffer = indexBuffer();
        if (buffer == null) {
            return 0;
        }
        int toSkip = (int) Math.min(n, buffer.remaining());
        buffer.position(buffer.position() + toSkip);
        return toSkip;
    }

    private ByteBuffer indexBuffer() {
        if (index >= bufferList.size()) {
            return null;
        }
        ByteBuffer buffer = bufferList.get(index);
        if (buffer.remaining() > 0) {
            return buffer;
        }
        index++;
        if (index >= bufferList.size()) {
            return null;
        }
        return bufferList.get(index);
    }

    @Override
    public synchronized void close() {
        bufferList.clear();
    }
}
