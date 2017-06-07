package net.dongliu.byproxy.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Extend ByteArrayOutputStream, to allow use internal buffer array
 *
 * @author Liu Dong
 */
class ByteArrayOutputStreamEx extends ByteArrayOutputStream {

    /**
     * Wrap internal byte array as byte buffer
     */
    public synchronized ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(buf, 0, count);
    }

    /**
     * Wrap internal byte array as input stream
     */
    public synchronized InputStream asInputStream() {
        return new ByteArrayInputStream(buf, 0, count);
    }
}
