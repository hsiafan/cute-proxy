package net.dongliu.proxy.store;

import java.nio.ByteBuffer;

/**
 * DataStore backend by OffHeap Memory
 */
class OffHeapStore extends DataStore {

    public OffHeapStore(int size) {
        super(createBuffer(size));
    }

    private static ByteBuffer createBuffer(int size) {
        return ByteBuffer.allocateDirect(size);
    }
}
