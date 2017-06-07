package net.dongliu.byproxy.parser;

import java.io.IOException;

/**
 * @author Liu Dong
 */
public interface NumberReader {

    int read() throws IOException;

    /**
     * Read two byte unsigned value
     */
    default int readUInt16() throws IOException {
        return (this.read() << 8) + this.read();
    }

    /**
     * Read three byte unsigned value
     */
    default int readUInt24() throws IOException {
        return (this.read() << 16) + (this.read() << 8) + this.read();
    }

    /**
     * Read four byte unsigned value
     */
    default int readUInt32() throws IOException {
        return (this.read() << 24) + (this.read() << 16) + (this.read() << 8) + this.read();
    }

    /**
     * Read eight byte unsigned value
     */
    default long readUInt64() throws IOException {
        return ((long) this.read() << 56)
                + ((long) this.read() << 48)
                + ((long) this.read() << 40)
                + ((long) this.read() << 32)
                + ((long) this.read() << 24)
                + ((long) this.read() << 16)
                + ((long) this.read() << 8)
                + ((long) this.read());
    }
}
