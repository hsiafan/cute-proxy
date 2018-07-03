package net.dongliu.proxy.store;

import java.nio.ByteBuffer;

/**
 * A piece of data
 */
class Chunk {
    private DataStore store;
    private int offset;
    private int size;

    public Chunk(DataStore store, int offset, int size) {
        this.store = store;
        this.offset = offset;
        this.size = size;
    }

    public ByteBuffer read() {
        return store.read(offset, size);
    }

    public DataStore getStore() {
        return store;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }
}
